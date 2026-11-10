"""
Аниме-лица для голов-кубов. Рисуем на холсте в S раз крупнее с плавными линиями, потом уменьшаем (LANCZOS)
до размера грани — получается чистая «аниме»-графика без лесенок.

Параметры лица (dict):
  skin, hair, eye — цвета (RGBA);  sex — 'f' | 'm';
  expr — 'sly' (Кармилла: прищур, ухмылка с клыками), 'stern' (Элеонора: сосредоточенный взгляд),
         'grin' (широкая улыбка), 'soft' (мягкая улыбка), 'calm' (спокойный), 'grim' (суровый);
  bangs — 'hime' (прямая чёлка и пряди по бокам), 'swept' (косая чёлка), 'messy' (лохматые пряди), 'short' (короткая);
  glasses, fangs, scar, freckles — дополнительно.
"""
import math
from PIL import Image, ImageDraw, ImageFilter

S = 4  # суперсэмплинг


def _c(c, k=1.0, a=None):
    r = tuple(max(0, min(255, int(v * k))) for v in c[:3])
    return r + ((c[3] if len(c) > 3 else 255) if a is None else a,)


def _mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


class Canvas:
    def __init__(self, w, h):
        self.W, self.H = w * S, h * S
        self.img = Image.new('RGBA', (self.W, self.H), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)

    def p(self, u, v): return (u * self.W, v * self.H)
    def lw(self, k): return max(1, int(round(k * self.W / 90)))   # толщина линии в «пикселях лица 90px»

    def layer(self):
        im = Image.new('RGBA', (self.W, self.H), (0, 0, 0, 0)); return im, ImageDraw.Draw(im)

    def over(self, im, mask=None):
        if mask is not None:
            a = im.split()[3]
            from PIL import ImageChops
            im = im.copy(); im.putalpha(ImageChops.multiply(a, mask))
        self.img.alpha_composite(im)


def _curve(cv, pts, n=24):
    """Кривая Безье по контрольным точкам (u, v) -> список точек холста."""
    out = []
    for i in range(n + 1):
        t = i / n; q = list(pts)
        while len(q) > 1: q = [(q[k][0] + (q[k + 1][0] - q[k][0]) * t, q[k][1] + (q[k + 1][1] - q[k][1]) * t) for k in range(len(q) - 1)]
        out.append(cv.p(*q[0]))
    return out


# ------------------------------------------------------------------ глаз
def _eye(cv, cx, cy, side, f):
    """Аниме-глаз «новой рисовки»: крупная радужка с градиентом и ободком, кольцо, звёздный блик, мягкие ресницы."""
    from PIL import ImageChops
    fem = f['sex'] == 'f'; expr = f['expr']
    ew = 0.30 if fem else 0.25
    eh = {'sly': 0.21, 'stern': 0.23, 'grin': 0.07, 'grim': 0.15}.get(expr, 0.29 if fem else 0.20)
    lash = f['lash']
    if expr == 'grin':
        pts = _curve(cv, [(cx - ew / 2, cy + 0.02), (cx, cy - 0.07), (cx + ew / 2, cy + 0.02)])
        cv.d.line(pts, fill=lash, width=cv.lw(2.6), joint='curve'); return
    inner, outer = cx - side * ew / 2, cx + side * ew / 2
    top = cy - eh / 2; bot = cy + eh / 2
    tilt = {'stern': 0.035, 'grim': 0.04, 'sly': -0.02}.get(expr, -0.008 if fem else 0.0)
    lid_drop = {'sly': 0.04}.get(expr, 0.0)
    upper = [(inner, cy + tilt * 0.5), (inner + side * ew * 0.15, top - 0.015 + tilt + lid_drop), (outer - side * ew * 0.22, top - 0.03 + lid_drop), (outer + side * 0.01, cy - eh * 0.15 + lid_drop * 0.5)]
    lower = [(outer + side * 0.01, cy - eh * 0.15 + lid_drop * 0.5), (outer - side * ew * 0.18, bot + 0.012), (inner + side * ew * 0.25, bot + 0.008), (inner, cy + tilt * 0.5)]
    shape = _curve(cv, upper, 24) + _curve(cv, lower, 24)[1:]
    mask = Image.new('L', (cv.W, cv.H), 0); ImageDraw.Draw(mask).polygon(shape, fill=255)
    lay, d = cv.layer()
    d.polygon(shape, fill=(250, 248, 252, 255))
    eye = f['eye']
    iw, ih = ew * (0.66 if fem else 0.56), eh * 1.18 + 0.03
    icx, icy = cx + side * 0.006, cy + 0.012
    ix0, iy0 = cv.p(icx - iw / 2, icy - ih / 2); ix1, iy1 = cv.p(icx + iw / 2, icy + ih / 2)
    # радужка: вертикальный градиент (тёмный верх → светлый низ)
    im2 = Image.new('L', (cv.W, cv.H), 0); ImageDraw.Draw(im2).ellipse([ix0, iy0, ix1, iy1], fill=255)
    grad = Image.new('RGBA', (cv.W, cv.H), (0, 0, 0, 0)); gd = ImageDraw.Draw(grad)
    for y in range(int(iy0), int(iy1) + 1):
        t = max(0.0, (y - iy0) / max(1, iy1 - iy0))
        c = _mix(_c(eye, 0.35), _c(eye, 1.5), min(1, t ** 1.2 * 1.1))
        gd.line([(ix0, y), (ix1, y)], fill=c)
    lay.paste(grad, (0, 0), im2)
    # внутреннее светлое кольцо и тёмный ободок
    cw, ch = (ix1 - ix0), (iy1 - iy0)
    d.ellipse([ix0 + cw * 0.18, iy0 + ch * 0.2, ix1 - cw * 0.18, iy1 - ch * 0.16], outline=_c(eye, 1.7, 160), width=cv.lw(0.9))
    d.ellipse([ix0, iy0, ix1, iy1], outline=_c(eye, 0.25), width=cv.lw(1.3))
    # зрачок
    pw = iw * (0.16 if f.get('fangs') else 0.30); ph = ih * (0.58 if f.get('fangs') else 0.40)
    px0, py0 = cv.p(icx - pw / 2, icy - ph / 2 - 0.006); px1, py1 = cv.p(icx + pw / 2, icy + ph / 2 - 0.006)
    d.ellipse([px0, py0, px1, py1], fill=_c(eye, 0.15))
    # нижний отсвет — светлый полумесяц
    gl = Image.new('RGBA', (cv.W, cv.H), (0, 0, 0, 0)); gld = ImageDraw.Draw(gl)
    gld.ellipse([ix0 + cw * 0.2, iy0 + ch * 0.55, ix1 - cw * 0.2, iy1 - ch * 0.06], fill=_c(eye, 1.9, 120))
    lay.alpha_composite(gl.filter(ImageFilter.GaussianBlur(cv.W * 0.006)))
    # блики: «звёздочка» и точки
    hx, hy = cv.p(icx - side * iw * 0.12, icy - ih * 0.2); r = iw * cv.W * 0.17
    d.polygon([(hx, hy - r * 1.4), (hx + r * 0.3, hy - r * 0.3), (hx + r * 1.2, hy), (hx + r * 0.3, hy + r * 0.3), (hx, hy + r * 1.3), (hx - r * 0.3, hy + r * 0.3), (hx - r * 1.2, hy), (hx - r * 0.3, hy - r * 0.3)], fill=(255, 255, 255, 255))
    for (dx, dy, rr) in ((0.22, 0.24, 0.32), (-0.18, 0.3, 0.2)):
        qx, qy = cv.p(icx + side * iw * dx, icy + ih * dy); q = r * rr
        d.ellipse([qx - q, qy - q, qx + q, qy + q], fill=(255, 255, 255, 230))
    # тень от века на белке (сиреневая)
    sh = Image.new('RGBA', (cv.W, cv.H), (0, 0, 0, 0)); ImageDraw.Draw(sh).line(_curve(cv, upper, 24), fill=(150, 130, 175, 160), width=cv.lw(4.2))
    lay.alpha_composite(sh.filter(ImageFilter.GaussianBlur(cv.W * 0.004)))
    cv.over(lay, mask)
    # верхнее веко: толще к внешнему углу, тёмно-коричневое
    up = _curve(cv, upper, 30)
    for k in range(len(up) - 1):
        t = k / (len(up) - 1)
        wdt = cv.lw((1.2 + 2.4 * t) if fem else (1.1 + 1.3 * t))
        cv.d.line([up[k], up[k + 1]], fill=lash, width=wdt)
    if fem:   # ресницы у внешнего угла
        for k, (dx, dy) in enumerate(((0.05, -0.035), (0.06, -0.012), (0.045, 0.012))):
            bx, by = up[-1 - k * 3]
            cv.d.line([bx, by, bx + side * dx * cv.W, by + dy * cv.H], fill=lash, width=cv.lw(1.3 - k * 0.2))
    # нижнее веко — тонкая светлая линия и пара ресничек
    lw_pts = _curve(cv, lower, 24)
    cv.d.line(lw_pts[:14], fill=_c(lash, 1.9, 170), width=cv.lw(0.8))
    if fem:
        for k in (3, 6):
            bx, by = lw_pts[k]; cv.d.line([bx, by, bx + side * 0.012 * cv.W, by + 0.018 * cv.H], fill=_c(lash, 1.6, 180), width=cv.lw(0.7))
    # складка века
    if expr != 'sly':
        cr = [(inner + side * ew * 0.2, top - 0.045 + tilt), (outer - side * ew * 0.25, top - 0.065), (outer - side * 0.005, top - 0.04)]
        cv.d.line(_curve(cv, cr, 14), fill=_c(f['skin'], 0.72, 160), width=cv.lw(0.7))


class _Scaled:
    """Обёртка холста: увеличивает рисунок вокруг точки (mx, my) в k раз."""
    def __init__(self, cv, mx, my, k): self.cv, self.mx, self.my, self.k, self.d, self.W, self.H = cv, mx, my, k, cv.d, cv.W, cv.H
    def p(self, u, v): return self.cv.p(self.mx + (u - self.mx) * self.k, self.my + (v - self.my) * self.k)
    def lw(self, x): return self.cv.lw(x)


def _brow(cv, cx, cy, side, f):
    expr = f['expr']; fem = f['sex'] == 'f'
    inner, outer = cx - side * 0.11, cx + side * 0.12
    y = cy - (0.17 if fem else 0.15)
    # суровые/хмурые — внутренний конец ниже; хитрая — одна бровь приподнята
    di = {'stern': 0.045, 'grim': 0.05, 'sly': 0.0 if side < 0 else -0.02, 'soft': -0.01, 'grin': -0.02}.get(expr, 0.0)
    pts = _curve(cv, [(inner, y + di), (cx, y - 0.03 + di * 0.3), (outer, y + 0.01)], 12)
    cv.d.line(pts, fill=f['brow'], width=cv.lw(1.5 if fem else 2.6), joint='curve')


def _mouth(cv, f):
    expr = f['expr']; mx, my = 0.5, 0.87
    k = 1.6; p0 = cv.p
    cv = _Scaled(cv, mx, my, k)
    line = _c(f['skin'], 0.42); lip = (150, 40, 60, 255)
    if expr == 'sly':   # ухмылка уголком, приоткрытый рот и два клыка
        pts = _curve(cv, [(mx - 0.08, my - 0.005), (mx - 0.01, my + 0.03), (mx + 0.07, my + 0.005), (mx + 0.1, my - 0.03)], 16)
        cv.d.polygon(pts + [cv.p(mx + 0.04, my - 0.005), cv.p(mx - 0.04, my - 0.01)], fill=(120, 20, 36, 255))
        cv.d.line(pts, fill=(90, 10, 24, 255), width=cv.lw(1.3), joint='curve')
        for fx in (mx - 0.045, mx + 0.04):
            a, b = cv.p(fx - 0.018, my - 0.006), cv.p(fx + 0.018, my - 0.006); t = cv.p(fx, my + 0.045)
            cv.d.polygon([a, b, t], fill=(255, 255, 255, 255), outline=(200, 180, 190, 255))
        # капелька крови в уголке
        bx, by = cv.p(mx + 0.1, my - 0.02); r = 0.012 * cv.W
        cv.d.ellipse([bx - r, by, bx + r, by + 2.4 * r], fill=(190, 0, 24, 255))
    elif expr == 'stern':
        pts = _curve(cv, [(mx - 0.06, my + 0.008), (mx, my - 0.004), (mx + 0.06, my + 0.008)], 10)
        cv.d.line(pts, fill=line, width=cv.lw(1.4), joint='curve')
    elif expr == 'grim':
        cv.d.line(_curve(cv, [(mx - 0.07, my + 0.012), (mx, my), (mx + 0.07, my + 0.012)], 10), fill=line, width=cv.lw(1.8))
    elif expr == 'grin':   # открытый «D»-рот
        top = _curve(cv, [(mx - 0.1, my - 0.02), (mx, my - 0.03), (mx + 0.1, my - 0.02)], 10)
        bot = _curve(cv, [(mx + 0.1, my - 0.02), (mx + 0.04, my + 0.07), (mx - 0.04, my + 0.07), (mx - 0.1, my - 0.02)], 14)
        cv.d.polygon(top + bot, fill=(110, 30, 40, 255))
        tx, ty = cv.p(mx, my + 0.04); cv.d.ellipse([tx - 0.045 * cv.W, ty - 0.012 * cv.H, tx + 0.045 * cv.W, ty + 0.03 * cv.H], fill=(220, 110, 120, 255))
        cv.d.polygon(top + [cv.p(mx + 0.08, my), cv.p(mx - 0.08, my)], fill=(255, 255, 255, 255))
        cv.d.line(top + bot, fill=_c(f['skin'], 0.35), width=cv.lw(1.1))
    elif expr == 'soft':
        cv.d.line(_curve(cv, [(mx - 0.07, my - 0.01), (mx, my + 0.03), (mx + 0.07, my - 0.01)], 12), fill=line, width=cv.lw(1.4), joint='curve')
    else:
        cv.d.line(_curve(cv, [(mx - 0.04, my), (mx, my + 0.01), (mx + 0.04, my)], 8), fill=line, width=cv.lw(1.3))


def _bangs(cv, f):
    hair = f['hair']; style = f['bangs']; dark = _c(hair, 0.62); light = _c(hair, 1.35)
    d = cv.d
    if style == 'hime':
        # прямая густая чёлка + длинные пряди-«химе» по бокам лица
        tips = [0.0] + [i / 9 for i in range(1, 9)] + [1.0]
        pts = [cv.p(0, 0)]
        for i, u in enumerate(tips):
            pts.append(cv.p(u, 0.33 + (0.025 if i % 2 else 0.0)))
        pts.append(cv.p(1, 0)); d.polygon(pts, fill=hair)
        for x0 in (0.0, 0.86):
            d.polygon([cv.p(x0, 0.2), cv.p(x0 + 0.14, 0.2), cv.p(x0 + 0.13, 0.95), cv.p(x0 + 0.07, 1.0), cv.p(x0 + 0.01, 0.95)], fill=hair)
            d.line([cv.p(x0 + 0.07, 0.3), cv.p(x0 + 0.07, 0.95)], fill=dark, width=cv.lw(1))
        for i in range(1, 9):
            d.line([cv.p(i / 9, 0.12), cv.p(i / 9 + 0.005, 0.33)], fill=dark, width=cv.lw(0.9))
    elif style == 'swept':
        # косая чёлка набок (Элеонора): длинная прядь с левого края уходит вниз вправо
        pts = [cv.p(0, 0), cv.p(1, 0), cv.p(1, 0.42), cv.p(0.9, 0.3), cv.p(0.8, 0.36), cv.p(0.66, 0.24), cv.p(0.55, 0.32), cv.p(0.42, 0.2),
               cv.p(0.3, 0.3), cv.p(0.18, 0.22), cv.p(0.1, 0.4), cv.p(0.0, 0.5)]
        d.polygon(pts, fill=hair)
        for a, b in (((0.17, 0.16), (0.1, 0.38)), ((0.38, 0.12), (0.3, 0.29)), ((0.62, 0.12), (0.55, 0.3)), ((0.87, 0.14), (0.82, 0.34))):
            d.line([cv.p(*a), cv.p(*b)], fill=dark, width=cv.lw(1))
        for x0 in (0.0, 0.89):
            d.polygon([cv.p(x0, 0.3), cv.p(x0 + 0.11, 0.35), cv.p(x0 + 0.09, 0.8), cv.p(x0 + 0.03, 0.85)], fill=hair)
    elif style == 'messy':
        n = 7; pts = [cv.p(0, 0)]
        for i in range(n + 1):
            u = i / n; depth = 0.27 + 0.09 * ((i * 37) % 5) / 4
            pts.append(cv.p(u, 0.12 if i in (0, n) else 0.15)); pts.append(cv.p(min(1, u + 0.5 / n) - (0.03 if i % 2 else -0.02), depth))
        pts.append(cv.p(1, 0.32)); pts.append(cv.p(1, 0)); d.polygon(pts, fill=hair)
        for x0 in (0.0, 0.9):
            d.polygon([cv.p(x0, 0.2), cv.p(x0 + 0.1, 0.25), cv.p(x0 + 0.06, 0.56), cv.p(x0 + 0.02, 0.5)], fill=hair)
    else:  # short
        pts = [cv.p(0, 0), cv.p(1, 0), cv.p(1, 0.3), cv.p(0.82, 0.22), cv.p(0.7, 0.27), cv.p(0.5, 0.2), cv.p(0.32, 0.26), cv.p(0.18, 0.2), cv.p(0, 0.3)]
        d.polygon(pts, fill=hair)
        for x0 in (0.0, 0.93):
            d.rectangle([cv.p(x0, 0.2), cv.p(x0 + 0.07, 0.48)], fill=hair)
    # блик «нимб» на волосах
    d.line(_curve(cv, [(0.12, 0.1), (0.5, 0.05), (0.88, 0.1)], 16), fill=light, width=cv.lw(1.6))


def face_image(f, w, h):
    """Возвращает RGBA-картинку лица размером w×h текселей."""
    f = dict(f); f.setdefault('lash', (28, 16, 22, 255)); f.setdefault('brow', _c(f['hair'], 0.7))
    cv = Canvas(w, h)
    skin = f['skin']
    cv.d.rectangle([0, 0, cv.W, cv.H], fill=skin)
    # мягкая тень по бокам «щёк» и под чёлкой
    lay, d = cv.layer()
    d.rectangle([0, 0, cv.W, 0.36 * cv.H], fill=_c(skin, 0.85, 120))
    for x0 in (-0.08, 0.92):
        d.ellipse([cv.p(x0, 0.45), cv.p(x0 + 0.16, 1.1)], fill=_c(skin, 0.88, 140))
    cv.over(lay.filter(ImageFilter.GaussianBlur(cv.W * 0.03)))
    # румянец
    if f.get('blush', True):
        lay, d = cv.layer()
        for cx in (0.24, 0.76):
            d.ellipse([cv.p(cx - 0.09, 0.73), cv.p(cx + 0.09, 0.79)], fill=f.get('blush_c', (240, 120, 140, 120)))
        cv.over(lay.filter(ImageFilter.GaussianBlur(cv.W * 0.015)))
        if f['sex'] == 'f':
            for cx in (0.24, 0.76):
                for k in (-1, 0, 1):
                    a = cv.p(cx + k * 0.035, 0.74); b = cv.p(cx + k * 0.035 - 0.015, 0.78)
                    cv.d.line([a, b], fill=(225, 100, 120, 160), width=cv.lw(0.6))
    if f.get('freckles'):
        for cx in (0.22, 0.78):
            for k, (du, dv) in enumerate(((-0.04, 0.0), (0.0, 0.015), (0.04, -0.005), (0.02, 0.03))):
                x, y = cv.p(cx + du, 0.72 + dv); r = 0.007 * cv.W
                cv.d.ellipse([x - r, y - r, x + r, y + r], fill=_c(skin, 0.68))
    ey = 0.6
    for side, cx in ((-1, 0.29), (1, 0.71)):
        _eye(cv, cx, ey, side if cx > 0.5 else -1, f)
    # нос — маленькая тень
    cv.d.line([cv.p(0.505, 0.745), cv.p(0.49, 0.77)], fill=_c(skin, 0.7), width=cv.lw(1.0))
    _mouth(cv, f)
    if f.get('scar'):
        cv.d.line([cv.p(0.66, 0.4), cv.p(0.8, 0.7)], fill=(190, 110, 110, 255), width=cv.lw(1.2))
        for k in (0.48, 0.58):
            a = cv.p(0.66 + 0.14 * (k - 0.4) / 0.3 - 0.025, 0.4 + 0.3 * (k - 0.4) / 0.3); cv.d.line([a, (a[0] + 0.05 * cv.W, a[1])], fill=(190, 110, 110, 255), width=cv.lw(0.8))
    _bangs(cv, f)
    for side, cx in ((-1, 0.29), (1, 0.71)):
        _brow(cv, cx, ey, -1 if cx < 0.5 else 1, f)
    if f.get('glasses'):
        fr = f.get('glasses_c', (60, 60, 70, 255))
        for cx in (0.29, 0.71):
            cv.d.rounded_rectangle([cv.p(cx - 0.15, ey - 0.13), cv.p(cx + 0.15, ey + 0.13)], radius=0.04 * cv.W, outline=fr, width=cv.lw(1.5))
            lay, d = cv.layer(); d.line([cv.p(cx - 0.08, ey - 0.1), cv.p(cx - 0.12, ey - 0.02)], fill=(255, 255, 255, 140), width=cv.lw(1.6)); cv.over(lay)
        cv.d.line([cv.p(0.44, ey - 0.05), cv.p(0.56, ey - 0.05)], fill=fr, width=cv.lw(1.4))
    return cv.img.resize((w, h), Image.LANCZOS)
