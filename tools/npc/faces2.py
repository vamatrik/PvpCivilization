"""
Пять стилей лица (для выбора): 'pixel' — майнкрафтовский пиксель-скин, 'tall' — аниме с вертикальными глазами,
'chibi' — чиби, 'doll' — готическая кукла, 'paint' — мягкая акварельная прорисовка.
Параметры f — как в faces.py (skin, hair, eye, expr 'sly'|'stern', bangs, fangs, lash, brow, blush_c) + lip.
"""
import math
from PIL import Image, ImageDraw, ImageFilter
from faces import Canvas, _curve, _c, _mix, _bangs

# ===================================================================== общие части
def _base(cv, f, shade=True):
    skin = f['skin']
    cv.d.rectangle([0, 0, cv.W, cv.H], fill=skin)
    if shade:
        lay, d = cv.layer()
        d.rectangle([0, 0, cv.W, 0.36 * cv.H], fill=_c(skin, 0.86, 110))
        for x0 in (-0.08, 0.92): d.ellipse([cv.p(x0, 0.45), cv.p(x0 + 0.16, 1.1)], fill=_c(skin, 0.9, 120))
        cv.over(lay.filter(ImageFilter.GaussianBlur(cv.W * 0.03)))

def _blush(cv, f, cy=0.76, rx=0.09, ry=0.035, a=110, lines=True, xs=(0.24, 0.76)):
    lay, d = cv.layer()
    bc = f.get('blush_c', (240, 120, 140, 120))
    for cx in xs: d.ellipse([cv.p(cx - rx, cy - ry), cv.p(cx + rx, cy + ry)], fill=bc[:3] + (a,))
    cv.over(lay.filter(ImageFilter.GaussianBlur(cv.W * 0.014)))
    if lines:
        for cx in xs:
            for k in (-1, 0, 1):
                cv.d.line([cv.p(cx + k * 0.032, cy - 0.015), cv.p(cx + k * 0.032 - 0.014, cy + 0.02)], fill=(220, 100, 120, 150), width=cv.lw(0.6))

def _iris(cv, lay, d, icx, icy, iw, ih, eye, slit, rings=True):
    """Радужка: вертикальный градиент, ободок, зрачок (у вампирши — вертикальная щель), блики."""
    ix0, iy0 = cv.p(icx - iw / 2, icy - ih / 2); ix1, iy1 = cv.p(icx + iw / 2, icy + ih / 2)
    m = Image.new('L', (cv.W, cv.H), 0); ImageDraw.Draw(m).ellipse([ix0, iy0, ix1, iy1], fill=255)
    g = Image.new('RGBA', (cv.W, cv.H), (0, 0, 0, 0)); gd = ImageDraw.Draw(g)
    for y in range(int(iy0), int(iy1) + 1):
        t = max(0.0, (y - iy0) / max(1, iy1 - iy0))
        gd.line([(ix0, y), (ix1, y)], fill=_mix(_c(eye, 0.3), _c(eye, 1.6), min(1, t ** 1.1)))
    lay.paste(g, (0, 0), m)
    cw, ch = ix1 - ix0, iy1 - iy0
    if rings: d.ellipse([ix0 + cw * 0.17, iy0 + ch * 0.18, ix1 - cw * 0.17, iy1 - ch * 0.14], outline=_c(eye, 1.8, 150), width=cv.lw(0.8))
    d.ellipse([ix0, iy0, ix1, iy1], outline=_c(eye, 0.22), width=cv.lw(1.2))
    if slit: pw, ph = iw * 0.16, ih * 0.62
    else: pw, ph = iw * 0.34, ih * 0.42
    d.ellipse([cv.p(icx - pw / 2, icy - ph / 2 - 0.01), cv.p(icx + pw / 2, icy + ph / 2 - 0.01)], fill=_c(eye, 0.12))
    gl = Image.new('RGBA', (cv.W, cv.H), (0, 0, 0, 0)); ImageDraw.Draw(gl).ellipse([ix0 + cw * 0.18, iy0 + ch * 0.58, ix1 - cw * 0.18, iy1 - ch * 0.05], fill=_c(eye, 2.0, 120))
    lay.alpha_composite(gl.filter(ImageFilter.GaussianBlur(cv.W * 0.006)))
    return ix0, iy0, ix1, iy1

def _shine(cv, d, x, y, r, a=255):
    d.ellipse([x - r, y - r * 1.25, x + r, y + r * 1.25], fill=(255, 255, 255, a))

def _fangs(cv, mx, my, size=0.03, gap=0.036):
    """Ровные клыки: два одинаковых аккуратных треугольника симметрично от центра."""
    for sx in (-1, 1):
        fx = mx + sx * gap
        a, b, t = cv.p(fx - size * 0.55, my), cv.p(fx + size * 0.55, my), cv.p(fx, my + size * 1.5)
        cv.d.polygon([a, b, t], fill=(255, 255, 255, 255))
        cv.d.line([a, t, b], fill=(205, 185, 195, 255), width=cv.lw(0.5))

def _smirk(cv, f, mx=0.5, my=0.86, w=0.075, fangs=True, lip=None):
    line = (95, 18, 30, 255)
    pts = _curve(cv, [(mx - w, my - 0.006), (mx - w * 0.3, my + 0.016), (mx + w * 0.4, my + 0.012), (mx + w * 1.15, my - 0.018)], 16)
    if lip: cv.d.polygon(pts + [cv.p(mx + w * 0.5, my - 0.006), cv.p(mx - w * 0.5, my - 0.008)], fill=lip)
    cv.d.line(pts, fill=line, width=cv.lw(1.3), joint='curve')
    if fangs: _fangs(cv, mx + 0.005, my + 0.006)

def _stern_mouth(cv, mx=0.5, my=0.86, w=0.055, col=(150, 90, 95, 255)):
    cv.d.line(_curve(cv, [(mx - w, my + 0.006), (mx, my - 0.002), (mx + w, my + 0.006)], 10), fill=col, width=cv.lw(1.3), joint='curve')

def _brows(cv, f, xs, y, arch=0.025, w=0.11, thick=1.3, inner_down=0.0):
    for cx, side in zip(xs, (-1, 1)):
        inner, outer = cx - side * w, cx + side * w
        pts = _curve(cv, [(inner, y + inner_down), (cx, y - arch), (outer, y + 0.01)], 12)
        cv.d.line(pts, fill=f['brow'], width=cv.lw(thick), joint='curve')

# ===================================================================== 1. «майнкрафт» — пиксель-скин
def face_pixel(f, w, h):
    """Лицо как у HD-скина 16×16: пастельные «пиксели», вертикальные глаза с бликом и белком, румянец, ровные клыки."""
    N = 16
    skin = f['skin']; hair = f['hair']; eye = f['eye']; sly = f['expr'] == 'sly'; stern = f['expr'] == 'stern'
    g = [[skin] * N for _ in range(N)]
    hl, hd, hdd = _c(hair, 1.22), _c(hair, 0.8), _c(hair, 0.65)
    for x in range(N): g[5][x] = _c(skin, 0.93)                                # тень под чёлкой
    for y in range(9, N): g[y][0] = _c(skin, 0.95); g[y][N - 1] = _c(skin, 0.95)
    if f['bangs'] == 'hime':
        for y in range(5):
            for x in range(N): g[y][x] = hair
        for x in range(1, N - 1): g[4][x] = hair if x not in (5, 10) else hd   # прямая чёлка, лёгкие прорези
        for y in range(5, 14):
            g[y][0] = hair; g[y][1] = hair if y < 12 else skin; g[y][N - 1] = hair; g[y][N - 2] = hair if y < 12 else skin
            if y > 8: g[y][1] = hd; g[y][N - 2] = hd
        for x in range(2, N - 2, 3): g[1][x] = hl
    else:
        cut = [6, 6, 5, 5, 4, 5, 4, 4, 3, 4, 3, 3, 4, 3, 5, 6]                 # косая чёлка — длиннее слева
        for x in range(N):
            for y in range(cut[x] + 1): g[y][x] = hair
        for y in range(6, 12): g[y][0] = hair; g[y][N - 1] = hair
        for x in range(1, N - 1, 3): g[1][x] = hl
    # брови
    for cx0, side in ((3, -1), (10, 1)):
        by = 6; bc_ = _mix(skin, f['brow'], 0.75)
        g[by][cx0 + (0 if side < 0 else 1)] = bc_; g[by][cx0 + (1 if side < 0 else 2)] = bc_
        if stern: g[by][cx0 + (1 if side < 0 else 1)] = skin; g[by + 1][cx0 + (2 if side < 0 else 0)] = bc_; g[by][cx0 + (0 if side < 0 else 2)] = bc_
    # глаза 3×4: ресницы, блик, радужка тёмная→светлая, белок снизу у внешнего края
    lash = f['lash']
    for cx0, side in ((3, -1), (10, 1)):
        top = 8
        inner = cx0 + (2 if side < 0 else 0); outer = cx0 + (0 if side < 0 else 2)
        for x in range(cx0, cx0 + 3): g[top][x] = lash
        g[top][outer + side] = _mix(skin, lash, 0.7)                                       # ресничка-крылышко
        c_dark, c_mid, c_light = _c(eye, 0.5), _c(eye, 0.85), _c(eye, 1.35)
        rows = [c_dark, c_mid, c_light]
        for k, c in enumerate(rows):
            for x in range(cx0, cx0 + 3): g[top + 1 + k][x] = c
        g[top + 3][outer] = (240, 244, 250, 255)                                          # белок
        g[top + 1][cx0 + (0 if side < 0 else 2)] = (255, 255, 255, 255)                   # блик
        if f.get('fangs'): g[top + 2][cx0 + 1] = _c(eye, 0.22)                             # вертикальный зрачок вампирши
        if f.get('fangs'): g[top + 3][cx0 + 1] = _c(eye, 0.6)
        g[top + 4][cx0 + 1] = _mix(skin, lash, 0.25)                                       # нижнее веко
    # румянец
    bc = f.get('blush_c', (240, 120, 140, 255))
    for x in (2, 3, 12, 13): g[12][x] = _mix(skin, bc[:3] + (255,), 0.5)
    # нос — одна тень
    g[11][8] = _c(skin, 0.88)
    # рот
    if sly:
        for x in (6, 7, 8): g[13][x] = (120, 26, 40, 255)
        g[12][9] = (120, 26, 40, 255)
        if f.get('fangs'): g[14][6] = (255, 255, 255, 255); g[14][8] = (255, 255, 255, 255)   # два ровных клыка
    else:
        g[13][7] = _mix(skin, (190, 110, 115, 255), 0.7); g[13][8] = _mix(skin, (190, 110, 115, 255), 0.7)
    img = Image.new('RGBA', (N, N))
    for y in range(N):
        for x in range(N): img.putpixel((x, y), g[y][x][:3] + (255,))
    return img.resize((w, h), Image.NEAREST)

# ===================================================================== 2. аниме, вертикальные глаза
def _tall_eye(cv, cx, cy, side, f, ew=0.19, eh=0.34):
    sly = f['expr'] == 'sly'; stern = f['expr'] == 'stern'
    lay, d = cv.layer()
    top, bot = cy - eh / 2, cy + eh / 2
    inner, outer = cx - side * ew / 2, cx + side * ew / 2
    upper = [(inner, top + 0.04), (cx - side * ew * 0.1, top - 0.012), (outer, top + 0.02 + (0.02 if stern else 0)), (outer + side * 0.012, top + 0.07)]
    lower = [(outer + side * 0.012, top + 0.07), (outer + side * 0.004, bot - 0.02), (cx, bot + 0.008), (inner, bot - 0.03), (inner, top + 0.04)]
    shape = _curve(cv, upper, 20) + _curve(cv, lower, 24)[1:]
    mask = Image.new('L', (cv.W, cv.H), 0); ImageDraw.Draw(mask).polygon(shape, fill=255)
    d.polygon(shape, fill=(250, 248, 252, 255))
    ix0, iy0, ix1, iy1 = _iris(cv, lay, d, cx + side * 0.005, cy + 0.02, ew * 0.92, eh * 0.9, f['eye'], f.get('fangs'))
    _shine(cv, d, ix0 + (ix1 - ix0) * (0.32 if side < 0 else 0.62), iy0 + (iy1 - iy0) * 0.26, (ix1 - ix0) * 0.16)
    _shine(cv, d, ix0 + (ix1 - ix0) * (0.68 if side < 0 else 0.34), iy0 + (iy1 - iy0) * 0.7, (ix1 - ix0) * 0.07, 220)
    sh = Image.new('RGBA', (cv.W, cv.H), (0, 0, 0, 0)); ImageDraw.Draw(sh).line(_curve(cv, upper, 20), fill=(140, 120, 170, 170), width=cv.lw(5))
    lay.alpha_composite(sh.filter(ImageFilter.GaussianBlur(cv.W * 0.005)))
    if sly:   # тяжёлое веко сверху
        ImageDraw.Draw(lay).rectangle([cv.p(cx - ew, top - 0.05), cv.p(cx + ew, top + eh * 0.28)], fill=f['skin'])
    cv.over(lay, mask)
    up = _curve(cv, upper, 26)
    if sly: up = _curve(cv, [(inner, top + eh * 0.28), (cx, top + eh * 0.24), (outer + side * 0.012, top + eh * 0.3)], 20)
    for k in range(len(up) - 1):
        t = k / (len(up) - 1); cv.d.line([up[k], up[k + 1]], fill=f['lash'], width=cv.lw(1.6 + 2.6 * t))
    bx, by = up[-1]
    cv.d.line([bx, by, bx + side * 0.045 * cv.W, by - 0.03 * cv.H], fill=f['lash'], width=cv.lw(1.6))   # ресничка-крылышко
    cv.d.line(_curve(cv, lower, 24)[4:14], fill=_c(f['lash'], 1.8, 160), width=cv.lw(0.8))

def face_tall(f, w, h):
    cv = Canvas(w, h); _base(cv, f); _blush(cv, f, 0.79)
    for cx, side in ((0.3, -1), (0.7, 1)): _tall_eye(cv, cx, 0.6, side, f)
    cv.d.line([cv.p(0.505, 0.77), cv.p(0.495, 0.785)], fill=_c(f['skin'], 0.72), width=cv.lw(0.9))
    if f['expr'] == 'sly': _smirk(cv, f, my=0.865, w=0.06)
    else: _stern_mouth(cv, my=0.87, w=0.045)
    _bangs(cv, f)
    _brows(cv, f, (0.3, 0.7), 0.4, arch=0.02, w=0.09, thick=1.2, inner_down=0.03 if f['expr'] == 'stern' else 0)
    return cv.img.resize((w, h), Image.LANCZOS)

# ===================================================================== 3. чиби
def face_chibi(f, w, h):
    cv = Canvas(w, h); _base(cv, f); _blush(cv, f, 0.8, rx=0.11, ry=0.045, a=150)
    for cx, side in ((0.29, -1), (0.71, 1)):
        lay, d = cv.layer(); r = 0.14; cy = 0.64
        ix0, iy0, ix1, iy1 = _iris(cv, lay, d, cx, cy, r * 2, r * 2.15, f['eye'], f.get('fangs'))
        _shine(cv, d, ix0 + (ix1 - ix0) * 0.3, iy0 + (iy1 - iy0) * 0.3, (ix1 - ix0) * 0.2)
        _shine(cv, d, ix0 + (ix1 - ix0) * 0.7, iy0 + (iy1 - iy0) * 0.66, (ix1 - ix0) * 0.1)
        if f['expr'] == 'sly': ImageDraw.Draw(lay).rectangle([cv.p(cx - r - 0.02, cy - r - 0.04), cv.p(cx + r + 0.02, cy - r * 0.62)], fill=(0, 0, 0, 0))
        cv.over(lay)
        ytop = cy - r * (0.62 if f['expr'] == 'sly' else 1.05)
        cv.d.line(_curve(cv, [(cx - r - 0.01, ytop + 0.02), (cx, ytop - 0.02), (cx + r + 0.01, ytop + 0.02)], 16), fill=f['lash'], width=cv.lw(3.2))
    if f['expr'] == 'sly':   # «ω» и один маленький клык сбоку
        pts = _curve(cv, [(0.44, 0.85), (0.47, 0.875), (0.5, 0.855)], 10) + _curve(cv, [(0.5, 0.855), (0.53, 0.875), (0.56, 0.85)], 10)[1:]
        cv.d.line(pts, fill=(95, 18, 30, 255), width=cv.lw(1.2))
        a, b, t = cv.p(0.525, 0.866), cv.p(0.545, 0.866), cv.p(0.535, 0.895); cv.d.polygon([a, b, t], fill=(255, 255, 255, 255))
    else:
        cv.d.line(_curve(cv, [(0.47, 0.865), (0.5, 0.875), (0.53, 0.865)], 8), fill=(160, 90, 95, 255), width=cv.lw(1.1))
    _bangs(cv, f)
    return cv.img.resize((w, h), Image.LANCZOS)

# ===================================================================== 4. готическая кукла
def face_doll(f, w, h):
    cv = Canvas(w, h); _base(cv, f); _blush(cv, f, 0.8, a=70, lines=False)
    sly = f['expr'] == 'sly'
    for cx, side in ((0.3, -1), (0.7, 1)):
        lay, d = cv.layer(); cy = 0.6; ew, eh = 0.25, 0.2
        inner, outer = cx - side * ew / 2, cx + side * ew / 2
        upper = [(inner, cy + 0.02), (cx - side * 0.02, cy - eh / 2 - 0.02), (outer, cy - eh * 0.45 - 0.03)]
        lower = [(outer, cy - eh * 0.45 - 0.03), (cx + side * 0.04, cy + eh / 2), (inner, cy + 0.02)]
        shape = _curve(cv, upper, 18) + _curve(cv, lower, 18)[1:]
        mask = Image.new('L', (cv.W, cv.H), 0); ImageDraw.Draw(mask).polygon(shape, fill=255)
        d.polygon(shape, fill=(248, 244, 248, 255))
        ix0, iy0, ix1, iy1 = _iris(cv, lay, d, cx, cy + 0.008, 0.15, 0.21, f['eye'], f.get('fangs'))
        _shine(cv, d, ix0 + (ix1 - ix0) * 0.3, iy0 + (iy1 - iy0) * 0.3, (ix1 - ix0) * 0.13)
        if sly: ImageDraw.Draw(lay).rectangle([cv.p(cx - ew, cy - 0.15), cv.p(cx + ew, cy - eh * 0.32)], fill=f['skin'])
        cv.over(lay, mask)
        up = _curve(cv, upper if not sly else [(inner, cy + 0.0), (cx, cy - eh * 0.34), (outer, cy - eh * 0.45)], 22)
        for k in range(len(up) - 1): cv.d.line([up[k], up[k + 1]], fill=f['lash'], width=cv.lw(2.2 + 2.5 * k / len(up)))
        bx, by = up[-1]   # подводка-стрелка
        cv.d.polygon([(bx, by - cv.lw(2)), (bx + side * 0.07 * cv.W, by - 0.045 * cv.H), (bx, by + cv.lw(2))], fill=f['lash'])
        for k in range(3):   # точки нижних ресниц
            px, py = cv.p(cx + side * (0.02 + k * 0.03), cy + eh / 2 + 0.012 - k * 0.006); r = 0.006 * cv.W
            cv.d.ellipse([px - r, py - r, px + r, py + r], fill=_c(f['lash'], 1.4))
    if sly:   # родинка под глазом
        px, py = cv.p(0.78, 0.71); r = 0.009 * cv.W; cv.d.ellipse([px - r, py - r, px + r, py + r], fill=(60, 20, 30, 255))
    lip = f.get('lip', (130, 20, 40, 255) if sly else (200, 110, 120, 255))
    top = _curve(cv, [(0.43, 0.86), (0.47, 0.84), (0.5, 0.853), (0.53, 0.84), (0.57, 0.86)], 16)
    bot = _curve(cv, [(0.57, 0.86), (0.5, 0.895), (0.43, 0.86)], 14)
    cv.d.polygon(top + bot, fill=lip)
    cv.d.line(_curve(cv, [(0.43, 0.861), (0.5, 0.868), (0.57, 0.861)], 12), fill=_c(lip, 0.55), width=cv.lw(0.8))
    lay, d = cv.layer(); d.ellipse([cv.p(0.48, 0.872), cv.p(0.52, 0.884)], fill=(255, 255, 255, 120)); cv.over(lay)
    if f.get('fangs'): _fangs(cv, 0.5, 0.866, size=0.016, gap=0.03)
    _bangs(cv, f)
    _brows(cv, f, (0.3, 0.7), 0.44, arch=0.035, w=0.11, thick=0.9, inner_down=0.03 if f['expr'] == 'stern' else 0)
    return cv.img.resize((w, h), Image.LANCZOS)

# ===================================================================== 5. мягкая акварель (мой)
def face_paint(f, w, h):
    cv = Canvas(w, h); _base(cv, f)
    # мягкий свет на лбу и щеках
    lay, d = cv.layer(); d.ellipse([cv.p(0.2, 0.25), cv.p(0.8, 0.75)], fill=(255, 250, 245, 60)); cv.over(lay.filter(ImageFilter.GaussianBlur(cv.W * 0.06)))
    _blush(cv, f, 0.78, rx=0.1, ry=0.04, a=90, lines=False)
    sly = f['expr'] == 'sly'
    for cx, side in ((0.3, -1), (0.7, 1)):
        cy = 0.6; ew, eh = 0.2, 0.25
        lay, d = cv.layer()
        upper = [(cx - side * ew / 2, cy - 0.01), (cx - side * 0.03, cy - eh / 2 - 0.01), (cx + side * ew / 2, cy - eh * 0.32)]
        lower = [(cx + side * ew / 2, cy - eh * 0.32), (cx + side * ew * 0.4, cy + eh * 0.42), (cx, cy + eh / 2), (cx - side * ew / 2, cy - 0.01)]
        shape = _curve(cv, upper, 20) + _curve(cv, lower, 20)[1:]
        mask = Image.new('L', (cv.W, cv.H), 0); ImageDraw.Draw(mask).polygon(shape, fill=255)
        d.polygon(shape, fill=(250, 248, 252, 255))
        ix0, iy0, ix1, iy1 = _iris(cv, lay, d, cx + side * 0.004, cy + 0.015, ew * 0.78, eh * 0.95, f['eye'], f.get('fangs'))
        # переливы в радужке: ещё одно светлое кольцо и цветные искры
        dd = ImageDraw.Draw(lay)
        for k in range(6):
            a = k * math.pi / 3 + 0.3
            px = (ix0 + ix1) / 2 + math.cos(a) * (ix1 - ix0) * 0.3; py = (iy0 + iy1) / 2 + math.sin(a) * (iy1 - iy0) * 0.3
            r = (ix1 - ix0) * 0.03; dd.ellipse([px - r, py - r, px + r, py + r], fill=_c(f['eye'], 2.2, 160))
        _shine(cv, dd, ix0 + (ix1 - ix0) * (0.3 if side < 0 else 0.6), iy0 + (iy1 - iy0) * 0.28, (ix1 - ix0) * 0.15)
        sh = Image.new('RGBA', (cv.W, cv.H), (0, 0, 0, 0)); ImageDraw.Draw(sh).line(_curve(cv, upper, 20), fill=(120, 100, 160, 190), width=cv.lw(7))
        lay.alpha_composite(sh.filter(ImageFilter.GaussianBlur(cv.W * 0.008)))
        if sly: dd.rectangle([cv.p(cx - ew, cy - 0.2), cv.p(cx + ew, cy - eh * 0.2)], fill=f['skin'])
        cv.over(lay, mask)
        # тень века над глазом (тени для век)
        lay, d = cv.layer(); d.ellipse([cv.p(cx - ew * 0.5, cy - eh * 0.62), cv.p(cx + ew * 0.55, cy - eh * 0.36)], fill=f.get('shadow', (170, 90, 130, 38))); cv.over(lay.filter(ImageFilter.GaussianBlur(cv.W * 0.01)))
        up = _curve(cv, upper if not sly else [(cx - side * ew / 2, cy - 0.01), (cx, cy - eh * 0.2), (cx + side * ew / 2, cy - eh * 0.3)], 24)
        for k in range(len(up) - 1): cv.d.line([up[k], up[k + 1]], fill=f['lash'], width=cv.lw(1.4 + 2.2 * k / len(up)))
        for k, (dx, dy) in enumerate(((0.04, -0.035), (0.05, -0.012))):
            bx, by = up[-1 - k * 4]; cv.d.line([bx, by, bx + side * dx * cv.W, by + dy * cv.H], fill=f['lash'], width=cv.lw(1.1))
    cv.d.line([cv.p(0.505, 0.765), cv.p(0.495, 0.78)], fill=_c(f['skin'], 0.75), width=cv.lw(0.8))
    lip = (175, 45, 65, 255) if sly else (205, 120, 125, 255)
    if sly: _smirk(cv, f, my=0.862, w=0.06, lip=lip)
    else:
        cv.d.polygon(_curve(cv, [(0.46, 0.865), (0.5, 0.858), (0.54, 0.865)], 8) + _curve(cv, [(0.54, 0.865), (0.5, 0.884), (0.46, 0.865)], 8), fill=lip)
    _bangs(cv, f)
    _brows(cv, f, (0.3, 0.7), 0.42, arch=0.025, w=0.1, thick=1.1, inner_down=0.035 if f['expr'] == 'stern' else 0)
    return cv.img.resize((w, h), Image.LANCZOS)

STYLES = {'pixel': face_pixel, 'tall': face_tall, 'chibi': face_chibi, 'doll': face_doll, 'paint': face_paint}

def face_image(f, w, h, style):
    f = dict(f); f.setdefault('lash', (28, 16, 22, 255)); f.setdefault('brow', _c(f['hair'], 0.7))
    return STYLES[style](f, w, h)


# ===================================================================== v2: вертикальные глаза, пропорции аниме-лица
def _tall2_eye(cv, cx, cy, side, f):
    """Высокий глаз: верх века — дуга с утолщением к внешнему краю и хвостиком, радужка почти во весь глаз,
    тёмная верхняя половина (тень от века), светлая нижняя, зрачок, два блика + искорка."""
    sly = f['expr'] == 'sly'; stern = f['expr'] == 'stern'
    ew, eh = 0.205, 0.33
    lay, d = cv.layer()
    top, bot = cy - eh / 2, cy + eh / 2
    inner, outer = cx - side * ew / 2, cx + side * ew / 2
    upper = [(inner - side * 0.005, top + 0.06), (inner + side * ew * 0.12, top - 0.005), (outer - side * ew * 0.18, top - 0.008 + (0.02 if stern else 0)), (outer + side * 0.014, top + 0.055 + (0.01 if stern else 0))]
    lower = [(outer + side * 0.014, top + 0.055), (outer + side * 0.006, bot - 0.06), (cx + side * 0.02, bot + 0.004), (inner + side * 0.01, bot - 0.035), (inner - side * 0.005, top + 0.06)]
    shape = _curve(cv, upper, 22) + _curve(cv, lower, 26)[1:]
    mask = Image.new('L', (cv.W, cv.H), 0); ImageDraw.Draw(mask).polygon(shape, fill=255)
    d.polygon(shape, fill=(250, 248, 252, 255))
    icx, icy, iw, ih = cx + side * 0.004, cy + 0.03, ew * 0.88, eh * 0.86
    ix0, iy0, ix1, iy1 = _iris(cv, lay, d, icx, icy, iw, ih, f['eye'], f.get('fangs'))
    # тень от века на радужке
    sh = Image.new('RGBA', (cv.W, cv.H), (0, 0, 0, 0)); ImageDraw.Draw(sh).ellipse([ix0 - 4, iy0 - (iy1 - iy0) * 0.25, ix1 + 4, iy0 + (iy1 - iy0) * 0.35], fill=_c(f['eye'], 0.18, 150))
    lay.alpha_composite(sh.filter(ImageFilter.GaussianBlur(cv.W * 0.01)))
    _shine(cv, d, ix0 + (ix1 - ix0) * (0.3 if side < 0 else 0.68), iy0 + (iy1 - iy0) * 0.3, (ix1 - ix0) * 0.15)
    _shine(cv, d, ix0 + (ix1 - ix0) * (0.68 if side < 0 else 0.32), iy0 + (iy1 - iy0) * 0.74, (ix1 - ix0) * 0.07, 210)
    # искорка-звёздочка
    sx, sy = ix0 + (ix1 - ix0) * 0.5, iy0 + (iy1 - iy0) * 0.18; r = (ix1 - ix0) * 0.07
    d.polygon([(sx, sy - r * 1.6), (sx + r * 0.35, sy - r * 0.35), (sx + r * 1.6, sy), (sx + r * 0.35, sy + r * 0.35), (sx, sy + r * 1.6), (sx - r * 0.35, sy + r * 0.35), (sx - r * 1.6, sy), (sx - r * 0.35, sy - r * 0.35)], fill=(255, 255, 255, 235))
    sh = Image.new('RGBA', (cv.W, cv.H), (0, 0, 0, 0)); ImageDraw.Draw(sh).line(_curve(cv, upper, 22), fill=(130, 110, 165, 180), width=cv.lw(5))
    lay.alpha_composite(sh.filter(ImageFilter.GaussianBlur(cv.W * 0.005)))
    if sly: ImageDraw.Draw(lay).rectangle([cv.p(cx - ew, top - 0.06), cv.p(cx + ew, top + eh * 0.2)], fill=f['skin'])
    cv.over(lay, mask)
    up = _curve(cv, upper if not sly else [(inner, top + eh * 0.24), (cx - side * 0.02, top + eh * 0.16), (outer + side * 0.014, top + eh * 0.22)], 26)
    for k in range(len(up) - 1):
        t = k / (len(up) - 1); cv.d.line([up[k], up[k + 1]], fill=f['lash'], width=cv.lw(1.5 + 3.2 * t ** 1.3))
    bx, by = up[-1]
    cv.d.line([bx, by, bx + side * 0.04 * cv.W, by + 0.02 * cv.H], fill=f['lash'], width=cv.lw(2.0))           # хвостик вниз-наружу
    cv.d.line([up[-4][0], up[-4][1], up[-4][0] + side * 0.035 * cv.W, up[-4][1] - 0.03 * cv.H], fill=f['lash'], width=cv.lw(1.2))
    lw_ = _curve(cv, lower, 26)
    cv.d.line(lw_[1:9], fill=_c(f['lash'], 1.5, 200), width=cv.lw(1.0))

def face_tall2(f, w, h):
    cv = Canvas(w, h); _base(cv, f)
    _blush(cv, f, 0.8, rx=0.1, ry=0.035, a=120)
    for cx, side in ((0.285, -1), (0.715, 1)): _tall2_eye(cv, cx, 0.6, side, f)
    cv.d.line([cv.p(0.503, 0.79), cv.p(0.497, 0.8)], fill=_c(f['skin'], 0.7), width=cv.lw(0.8))   # нос — штрих
    if f['expr'] == 'sly': _smirk(cv, f, my=0.88, w=0.05)
    else: _stern_mouth(cv, my=0.89, w=0.04)
    _bangs(cv, f)
    _brows(cv, f, (0.285, 0.715), 0.37, arch=0.018, w=0.085, thick=1.0, inner_down=0.03 if f['expr'] == 'stern' else 0)
    return cv.img.resize((w, h), Image.LANCZOS)


# ===================================================================== v2: «как на картинке»
def face_ref(f, w, h):
    """По референсу: широко раскрытые глаза с белком вокруг округлой радужки, толстая верхняя ресничная линия с хвостиком,
    светлая радужка с тёмным ободком и вертикальным зрачком, искорки; широкий румянец, маленький нос, приоткрытый ротик;
    пряди чёлки свисают до глаз."""
    cv = Canvas(w, h); _base(cv, f)
    sly = f['expr'] == 'sly'; stern = f['expr'] == 'stern'
    # широкий румянец «полосой» под глазами
    lay, d = cv.layer()
    bc = f.get('blush_c', (240, 120, 140, 120))[:3]
    d.ellipse([cv.p(0.1, 0.7), cv.p(0.44, 0.84)], fill=bc + (120,)); d.ellipse([cv.p(0.56, 0.7), cv.p(0.9, 0.84)], fill=bc + (120,))
    d.rectangle([cv.p(0.3, 0.73), cv.p(0.7, 0.79)], fill=bc + (40,))
    cv.over(lay.filter(ImageFilter.GaussianBlur(cv.W * 0.03)))
    eye = f['eye']; light = _c(eye, 1.7)
    for cx, side in ((0.28, -1), (0.72, 1)):
        cy = 0.6; ew, eh = 0.25, 0.24
        lay, d = cv.layer()
        inner, outer = cx - side * ew / 2, cx + side * ew / 2
        upper = [(inner, cy + eh * 0.05), (inner + side * ew * 0.05, cy - eh * 0.62), (outer - side * ew * 0.08, cy - eh * 0.66 + (0.02 if stern else 0)), (outer + side * 0.006, cy - eh * 0.02)]
        lower = [(outer + side * 0.006, cy - eh * 0.02), (outer - side * ew * 0.04, cy + eh * 0.5), (cx, cy + eh * 0.58), (inner + side * ew * 0.06, cy + eh * 0.48), (inner, cy + eh * 0.05)]
        shape = _curve(cv, upper, 22) + _curve(cv, lower, 26)[1:]
        mask = Image.new('L', (cv.W, cv.H), 0); ImageDraw.Draw(mask).polygon(shape, fill=255)
        d.polygon(shape, fill=(252, 250, 255, 255))
        # белок: лёгкая сиреневая тень сверху
        sh = Image.new('RGBA', (cv.W, cv.H), (0, 0, 0, 0)); ImageDraw.Draw(sh).line(_curve(cv, upper, 22), fill=(150, 140, 190, 170), width=cv.lw(7))
        lay.alpha_composite(sh.filter(ImageFilter.GaussianBlur(cv.W * 0.008)))
        # радужка: округлая, меньше глаза (видно белок), светлая с тёмным ободком
        icx, icy = cx + side * 0.006, cy + 0.01
        iw, ih = 0.16, 0.2
        ix0, iy0 = cv.p(icx - iw / 2, icy - ih / 2); ix1, iy1 = cv.p(icx + iw / 2, icy + ih / 2)
        m = Image.new('L', (cv.W, cv.H), 0); ImageDraw.Draw(m).ellipse([ix0, iy0, ix1, iy1], fill=255)
        g = Image.new('RGBA', (cv.W, cv.H), (0, 0, 0, 0)); gd = ImageDraw.Draw(g)
        for y in range(int(iy0), int(iy1) + 1):
            t = (y - iy0) / max(1, iy1 - iy0)
            gd.line([(ix0, y), (ix1, y)], fill=_mix(_c(eye, 0.75), light, min(1, 0.25 + t)))
        lay.paste(g, (0, 0), m)
        dd = ImageDraw.Draw(lay)
        dd.ellipse([ix0, iy0, ix1, iy1], outline=_c(eye, 0.35), width=cv.lw(1.6))
        cw, ch = ix1 - ix0, iy1 - iy0
        dd.ellipse([ix0 + cw * 0.14, iy0 + ch * 0.14, ix1 - cw * 0.14, iy1 - ch * 0.14], outline=_c(eye, 0.6, 140), width=cv.lw(0.6))
        # вертикальный зрачок-«ромб» (у вампирши — уже и темнее)
        pw = cw * (0.2 if f.get('fangs') else 0.3); ph = ch * (0.5 if f.get('fangs') else 0.42); pcx, pcy = (ix0 + ix1) / 2, (iy0 + iy1) / 2 + ch * 0.03
        dd.ellipse([pcx - pw / 2, pcy - ph / 2, pcx + pw / 2, pcy + ph / 2], fill=_c(eye, 0.3 if f.get('fangs') else 0.42))   # мягкий вертикальный зрачок
        # искорки: крестик сверху и точка
        sx, sy = ix0 + cw * 0.5, iy0 + ch * 0.16; r = cw * 0.09
        dd.polygon([(sx, sy - r * 1.7), (sx + r * 0.3, sy - r * 0.3), (sx + r * 1.2, sy), (sx + r * 0.3, sy + r * 0.3), (sx, sy + r * 1.5), (sx - r * 0.3, sy + r * 0.3), (sx - r * 1.2, sy), (sx - r * 0.3, sy - r * 0.3)], fill=(255, 255, 255, 255))
        _shine(cv, dd, ix0 + cw * (0.25 if side < 0 else 0.75), iy0 + ch * 0.62, cw * 0.06, 230)
        if sly: dd.rectangle([cv.p(cx - ew, cy - 0.2), cv.p(cx + ew, cy - eh * 0.28)], fill=f['skin'])
        cv.over(lay, mask)
        # верхняя ресничная линия: толстая, с хвостиком наружу
        up = _curve(cv, upper if not sly else [(inner, cy + 0.0), (cx - side * 0.02, cy - eh * 0.3), (outer + side * 0.01, cy - eh * 0.2)], 26)
        for k in range(len(up) - 1):
            t = k / (len(up) - 1); cv.d.line([up[k], up[k + 1]], fill=f['lash'], width=cv.lw(2.2 + 3.2 * t))
        bx, by = up[-1]
        cv.d.polygon([(bx, by - cv.lw(2.5)), (bx + side * 0.05 * cv.W, by + 0.012 * cv.H), (bx, by + cv.lw(2.5))], fill=f['lash'])
        for k in (6, 10, 14):   # отдельные реснички
            px, py = up[-k]; cv.d.line([px, py, px + side * 0.012 * cv.W, py - 0.025 * cv.H], fill=f['lash'], width=cv.lw(0.9))
        # нижняя линия — тонкая, от внешнего угла до середины
        lw_ = _curve(cv, lower, 26)
        cv.d.line(lw_[:11], fill=_c(f['lash'], 1.3, 220), width=cv.lw(1.1))
    # нос: маленький штрих и блик
    cv.d.line([cv.p(0.505, 0.778), cv.p(0.497, 0.788)], fill=_c(f['skin'], 0.7), width=cv.lw(0.8))
    # ротик: приоткрытый, светлые верхние зубки; у вампирши — два ровных клыка (big — вариант 4: крупнее и с контуром)
    big = f.get('_bigmouth', False); mid = f.get('_midmouth', False)
    mx, my = 0.5, 0.88
    k = 1.75 if big else 1.35 if mid else 1.0
    def P(u, v): return (mx + (u - mx) * k, my + (v - my) * k)
    if sly:
        mouth = _curve(cv, [P(mx - 0.04, my + 0.002), P(mx, my - 0.008), P(mx + 0.045, my - 0.01)], 12) + _curve(cv, [P(mx + 0.045, my - 0.01), P(mx, my + 0.026), P(mx - 0.04, my + 0.002)], 12)
    elif stern:
        mouth = _curve(cv, [P(mx - 0.03, my + 0.002), P(mx, my - 0.004), P(mx + 0.03, my + 0.002)], 10) + _curve(cv, [P(mx + 0.03, my + 0.002), P(mx, my + 0.012), P(mx - 0.03, my + 0.002)], 10)
    else:
        mouth = _curve(cv, [P(mx - 0.035, my), P(mx, my - 0.012), P(mx + 0.035, my)], 10) + _curve(cv, [P(mx + 0.035, my), P(mx, my + 0.02), P(mx - 0.035, my)], 10)
    inside = (130, 25, 40, 255) if sly else (175, 85, 95, 255) if stern else (150, 60, 75, 255)
    cv.d.polygon(mouth, fill=inside)
    if not stern and not sly:   # верхние зубки (у Кармиллы — нет: только клыки из-под губы)
        cv.d.polygon(_curve(cv, [P(mx - 0.03, my - 0.002), P(mx, my - 0.01), P(mx + 0.032, my - 0.006)], 10) + [cv.p(*P(mx + 0.027, my + 0.004)), cv.p(*P(mx - 0.026, my + 0.006))], fill=(250, 244, 244, 255))
    if big or mid:   # контур и блик на нижней губе (в среднем варианте — мягче)
        cv.d.line(mouth + [mouth[0]], fill=_c(inside, 0.6 if big else 0.78), width=cv.lw(1.1 if big else 0.75), joint='curve')
        lay, d = cv.layer(); d.ellipse([cv.p(*P(mx - 0.012, my + (0.016 if sly else 0.009))), cv.p(*P(mx + 0.012, my + (0.024 if sly else 0.014)))], fill=(255, 255, 255, 90 if big else 55)); cv.over(lay)
    if sly:   # два клыка выглядывают из-под губы вниз, за край рта
        fy = my + 0.012 * k
        for sx in (-1, 1):
            fx = mx + 0.003 + sx * 0.02 * k
            a, b, t = cv.p(fx - 0.009, fy - 0.006), cv.p(fx + 0.009, fy - 0.006), cv.p(fx + sx * 0.002, fy + 0.026)
            cv.d.polygon([a, b, t], fill=(255, 255, 255, 255))
            cv.d.line([a, t, b], fill=(200, 175, 185, 255), width=cv.lw(0.6))
    elif stern and not big:
        cv.d.line(_curve(cv, [(mx - 0.03, my + 0.004), (mx, my + 0.002), (mx + 0.03, my + 0.004)], 8), fill=(150, 60, 75, 255), width=cv.lw(1.2))
    _bangs(cv, f)
    # пряди чёлки до глаз: тонкие заострённые локоны поверх
    hair = f['hair']; dark = _c(hair, 0.7)
    for (x0, x1, tip, lean) in ((0.42, 0.56, 0.56, -0.02), (0.08, 0.2, 0.5, 0.03), (0.8, 0.92, 0.5, -0.03), (0.5, 0.6, 0.47, 0.03)):
        pts = [cv.p(x0, 0.25), cv.p(x1, 0.25), cv.p((x0 + x1) / 2 + lean + 0.02, tip - 0.05), cv.p((x0 + x1) / 2 + lean, tip)]
        cv.d.polygon(pts, fill=hair)
        cv.d.line([cv.p((x0 + x1) / 2, 0.27), cv.p((x0 + x1) / 2 + lean * 0.8, tip - 0.04)], fill=dark, width=cv.lw(0.8))
    _brows(cv, f, (0.28, 0.72), 0.39, arch=0.015, w=0.08, thick=0.8, inner_down=0.025 if stern else 0)
    return cv.img.resize((w, h), Image.LANCZOS)

STYLES['tall2'] = face_tall2
STYLES['ref'] = face_ref

def face_ref2(f, w, h):
    f = dict(f); f['_bigmouth'] = True
    return face_ref(f, w, h)
STYLES['ref2'] = face_ref2

def face_ref3(f, w, h):
    f = dict(f); f['_midmouth'] = True
    return face_ref(f, w, h)
STYLES['ref3'] = face_ref3
