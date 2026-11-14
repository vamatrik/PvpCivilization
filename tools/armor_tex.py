"""0.10.5: свои текстуры брони (layer_1 — шлем/нагрудник/ботинки, layer_2 — поножи) для 4 сетов.
Раскладка — ванильная 64×32: голова (0,0), тело (16,16), руки (40,16), ноги (0,16)."""
import random
from PIL import Image
OUT = 'src/main/resources/assets/pvpciv/textures/models/armor/'

def c(h): return ((h >> 16) & 255, (h >> 8) & 255, h & 255, 255)
def shade(col, k): return tuple(max(0, min(255, int(v * k))) for v in col[:3]) + (col[3],)

class P:
    def __init__(s, seed): s.im = Image.new('RGBA', (64, 32), (0, 0, 0, 0)); s.r = random.Random(seed)
    def px(s, x, y, col): s.im.putpixel((x, y), col)
    def rect(s, x, y, w, h, col, noise=0.06):
        for j in range(h):
            for i in range(w): s.px(x + i, y + j, shade(col, 1 + s.r.uniform(-noise, noise)))
    def hline(s, x, y, w, col):
        for i in range(w): s.px(x + i, y, col)
    def vline(s, x, y, h, col):
        for j in range(h): s.px(x, y + j, col)
    def clear(s, x, y, w, h):
        for j in range(h):
            for i in range(w): s.px(x + i, y + j, (0, 0, 0, 0))
    # области кубов (u,v,w,h,d): верх, низ, право, перед, лево, зад
    def box(s, u, v, w, h, d):
        return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
                'left': (u + d + w, v + d, d, h), 'back': (u + d + w + d, v + d, w, h)}
    def fill_box(s, u, v, w, h, d, col, noise=0.06):
        for f, (x, y, ww, hh) in s.box(u, v, w, h, d).items(): s.rect(x, y, ww, hh, col, noise)

HEAD, BODY, ARM, LEG = (0, 0, 8, 8, 8), (16, 16, 8, 12, 4), (40, 16, 4, 12, 4), (0, 16, 4, 12, 4)

def plates(p, area, base, line, rows):   # горизонтальные пластины (ламели) со шнуровкой
    x, y, w, h = area
    for j in range(h):
        if j % rows == rows - 1: p.hline(x, y + j, w, line)
        else:
            k = 1.08 if j % rows == 0 else 1.0
            for i in range(w): p.px(x + i, y + j, shade(base, k + p.r.uniform(-0.05, 0.05)))

def faces(p, box, fn):
    for f, a in p.box(*box).items(): fn(f, a)

# ---------------------------------------------------------------- САМУРАЙ: красный лак, золотая шнуровка, чёрный шлем
def samurai():
    red, dark, gold, black, lace = c(0x8A1A1A), c(0x5A0E10), c(0xD8B048), c(0x1E1A1E), c(0xE8D8B0)
    p = P('sam1')
    # шлем: чёрный лак, золотой обод, лицо открыто
    p.fill_box(*HEAD, black, 0.08)
    fx, fy, fw, fh = p.box(*HEAD)['front']
    p.hline(fx, fy + 1, fw, gold); p.clear(fx + 1, fy + 3, 6, 5)
    for f in ('right', 'left', 'back'):
        x, y, w, h = p.box(*HEAD)[f]; p.hline(x, y + 1, w, gold)
        plates(p, (x, y + 4, w, 4), red, lace, 2)   # сикоро
    # тело: ламели
    faces(p, BODY, lambda f, a: plates(p, a, red, gold, 3) if f in ('front', 'back', 'right', 'left') else p.rect(*a, dark))
    x, y, w, h = p.box(*BODY)['front']; p.rect(x, y + 9, w, 2, black); p.hline(x, y + 9, w, gold)   # пояс
    # руки: наплечники-содэ сверху, котэ ниже
    def arm(f, a):
        x, y, w, h = a
        if f in ('top', 'bottom'): p.rect(*a, red); return
        plates(p, (x, y, w, 5), red, gold, 2); p.rect(x, y + 5, w, 7, black); p.hline(x, y + 8, w, gold)
    faces(p, ARM, arm)
    # ботинки: сунэатэ
    def boot(f, a):
        x, y, w, h = a
        if f in ('top', 'bottom'): p.rect(*a, black); return
        p.clear(x, y, w, 6); p.rect(x, y + 6, w, 6, black); p.vline(x + w // 2, y + 6, 4, gold) if f == 'front' else None
    faces(p, LEG, boot)
    p.im.save(OUT + 'samurai_layer_1.png')
    q = P('sam2')   # поножи: хайдатэ
    faces(q, LEG, lambda f, a: plates(q, a, red, lace, 3) if f not in ('top', 'bottom') else q.rect(*a, dark))
    faces(q, BODY, lambda f, a: q.rect(*a, black) if f not in ('top', 'bottom') else q.rect(*a, black))
    q.im.save(OUT + 'samurai_layer_2.png')

# ---------------------------------------------------------------- ЛИСИЙ КОСТЮМ: рыжий мех, белое брюхо, морда на шлеме
def fox():
    org, light, white, black, pink = c(0xE0782A), c(0xF09A50), c(0xF4ECE0), c(0x1A1414), c(0xE89AA0)
    def fur(p, a, col):
        x, y, w, h = a
        for j in range(h):
            for i in range(w):
                k = 1 + p.r.uniform(-0.07, 0.07) - (0.12 if (i + j // 2) % 3 == 0 else 0)
                p.px(x + i, y + j, shade(col, k))
    p = P('fox1')
    faces(p, HEAD, lambda f, a: fur(p, a, org))
    x, y, w, h = p.box(*HEAD)['front']   # морда: белые щёки, тёмные глаза, нос
    fur(p, (x, y + 4, w, 4), white)
    p.rect(x + 1, y + 3, 2, 1, black, 0); p.rect(x + 5, y + 3, 2, 1, black, 0)
    p.px(x + 2, y + 3, c(0xFFC040)); p.px(x + 5, y + 3, c(0xFFC040))
    p.rect(x + 3, y + 5, 2, 1, black, 0); p.px(x + 3, y + 6, pink); p.px(x + 4, y + 6, pink)
    p.hline(x, y, w, light)
    faces(p, BODY, lambda f, a: fur(p, a, org))
    x, y, w, h = p.box(*BODY)['front']; fur(p, (x + 2, y + 1, 4, 10), white)   # белое брюхо
    faces(p, ARM, lambda f, a: fur(p, a, org))
    for f in ('front', 'right', 'left', 'back'):
        x, y, w, h = p.box(*ARM)[f]; fur(p, (x, y + 9, w, 3), black)   # тёмные лапы
    faces(p, LEG, lambda f, a: fur(p, a, black) if f != 'top' else fur(p, a, org))
    for f in ('front', 'right', 'left', 'back'):
        x, y, w, h = p.box(*LEG)[f]; p.clear(x, y, w, 7)
    p.im.save(OUT + 'foxsuit_layer_1.png')
    q = P('fox2')
    faces(q, LEG, lambda f, a: fur(q, a, org))
    faces(q, BODY, lambda f, a: fur(q, a, org))
    q.im.save(OUT + 'foxsuit_layer_2.png')

# ---------------------------------------------------------------- ИНЕЕВЫЙ: ледяные пластины, белый мех, забрало
def rime():
    ice, deep, frost, fur_c, steel = c(0x7FB8E0), c(0x3A6A9A), c(0xE8F6FF), c(0xF0F0F0), c(0x8A9AAA)
    def icy(p, a):
        x, y, w, h = a
        for j in range(h):
            for i in range(w):
                k = 1 + p.r.uniform(-0.08, 0.08) + (0.18 if p.r.random() < 0.07 else 0)
                p.px(x + i, y + j, shade(deep if (i * 3 + j * 5) % 11 == 0 else ice, k))
    p = P('rime1')
    faces(p, HEAD, lambda f, a: icy(p, a))
    x, y, w, h = p.box(*HEAD)['front']
    p.clear(x + 1, y + 2, 6, 6); p.rect(x + 1, y + 3, 6, 1, steel, 0)   # забрало-щель
    p.hline(x, y + 1, w, frost)
    for f in ('right', 'left', 'back'):
        x, y, w, h = p.box(*HEAD)[f]; p.rect(x, y + 6, w, 2, fur_c)
    faces(p, BODY, lambda f, a: icy(p, a))
    x, y, w, h = p.box(*BODY)['front']; p.rect(x, y, w, 2, fur_c); p.vline(x + 3, y + 2, 9, frost); p.vline(x + 4, y + 2, 9, frost)
    faces(p, ARM, lambda f, a: icy(p, a))
    for f in ('front', 'right', 'left', 'back'):
        x, y, w, h = p.box(*ARM)[f]; p.rect(x, y, w, 2, fur_c)
    faces(p, LEG, lambda f, a: icy(p, a))
    for f in ('front', 'right', 'left', 'back'):
        x, y, w, h = p.box(*LEG)[f]; p.clear(x, y, w, 7); p.rect(x, y + 7, w, 1, fur_c)
    p.im.save(OUT + 'rime_layer_1.png')
    q = P('rime2'); faces(q, LEG, lambda f, a: icy(q, a)); faces(q, BODY, lambda f, a: icy(q, a))
    q.im.save(OUT + 'rime_layer_2.png')

# ---------------------------------------------------------------- ЛИКВИДАТОР: жёлтый прорезиненный костюм, чёрные полосы, капюшон
def hazmat():
    yel, dk, black, grey = c(0xD8C030), c(0xA89020), c(0x222222), c(0x5A5A5A)
    def rubber(p, a, col):
        x, y, w, h = a
        for j in range(h):
            for i in range(w): p.px(x + i, y + j, shade(col, 1 + p.r.uniform(-0.05, 0.05) - (0.1 if j % 4 == 3 else 0)))
    p = P('haz1')
    # 0.10.5: шлем — отдельный противогаз (3D-маска рисуется GasMaskLayer), от «капюшона» остаются только ремешки
    for f in ('right', 'left', 'back'):
        x, y, w, h = p.box(*HEAD)[f]; p.rect(x, y + 3, w, 1, black, 0); p.rect(x, y + 5, w, 1, black, 0)
    x, y, w, h = p.box(*HEAD)['top']; p.rect(x + 3, y, 2, h, black, 0)
    faces(p, BODY, lambda f, a: rubber(p, a, yel))
    x, y, w, h = p.box(*BODY)['front']
    for i in range(w): p.px(x + i, y + 6, black if (i // 2) % 2 == 0 else yel); p.px(x + i, y + 7, yel if (i // 2) % 2 == 0 else black)   # полосы опасности
    p.vline(x + 4, y, 6, grey)   # молния
    faces(p, ARM, lambda f, a: rubber(p, a, yel))
    for f in ('front', 'right', 'left', 'back'):
        x, y, w, h = p.box(*ARM)[f]; p.rect(x, y + 9, w, 3, black)   # перчатки
    faces(p, LEG, lambda f, a: rubber(p, a, black))
    for f in ('front', 'right', 'left', 'back'):
        x, y, w, h = p.box(*LEG)[f]; p.clear(x, y, w, 7)
    p.im.save(OUT + 'hazmat_layer_1.png')
    q = P('haz2'); faces(q, LEG, lambda f, a: rubber(q, a, yel)); faces(q, BODY, lambda f, a: rubber(q, a, dk))
    for f in ('front', 'right', 'left', 'back'):
        x, y, w, h = q.box(*LEG)[f]
        for i in range(w): q.px(x + i, y + 8, black)
    q.im.save(OUT + 'hazmat_layer_2.png')

samurai(); fox(); rime(); hazmat(); print('ok')
