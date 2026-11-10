#!/usr/bin/env python3
"""
Генератор ассетов мода «Лес»: модели GeckoLib (аниме-пропорции), текстуры персонажей, анимации, иконки предметов, языки.
Каждая грань куба получает свой участок текстуры (per-face UV) с плотностью D текселей на пиксель модели —
так у аниме-лиц хватает разрешения на большие глаза, блики и ресницы.
"""
import json, math, os, random
from PIL import Image, ImageDraw
import faces

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'forest')
TEX = 1024

def P(*p): return os.path.join(ROOT, *p)
def ensure(p): os.makedirs(os.path.dirname(p), exist_ok=True); return p
def hexc(s, a=255):
    s = s.lstrip('#'); return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)
def mul(c, k): return tuple(max(0, min(255, int(v * k))) for v in c[:3]) + (c[3],)
def mix(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)

# ---------------------------------------------------------------- упаковка граней в текстуру
class Packer:
    def __init__(self, w, h): self.w, self.h, self.x, self.y, self.row = w, h, 0, 0, 0
    def take(self, w, h):
        w, h = max(1, int(w)), max(1, int(h))
        if self.x + w > self.w: self.x, self.y, self.row = 0, self.y + self.row, 0
        if self.y + h > self.h: raise RuntimeError('текстура переполнена')
        r = (self.x, self.y); self.x += w; self.row = max(self.row, h); return r

FACES = ['north', 'south', 'east', 'west', 'up', 'down']

class Model:
    """Кость с кубами. painter(face, draw, rect) раскрашивает грань."""
    def __init__(self, ident, tex=TEX):
        self.tex = tex
        self.ident, self.bones, self.img = ident, [], Image.new('RGBA', (tex, tex), (0, 0, 0, 0))
        self.draw, self.pack = ImageDraw.Draw(self.img), Packer(tex, tex)
    def bone(self, name, parent, pivot, cubes=(), rotation=None):
        b = {'name': name, 'pivot': list(pivot)}
        if parent: b['parent'] = parent
        if rotation: b['rotation'] = rotation
        out = []
        for cube in cubes:
            origin, size, painter, dens = cube[:4]
            w, h, d = size
            dims = {'north': (w, h), 'south': (w, h), 'east': (d, h), 'west': (d, h), 'up': (w, d), 'down': (w, d)}
            uv = {}
            for fc in FACES:
                fw, fh = dims[fc]
                tw, th = max(1, round(fw * dens)), max(1, round(fh * dens))
                x, y = self.pack.take(tw, th)
                painter(fc, self.draw, (x, y, x + tw, y + th))
                uv[fc] = {'uv': [x, y], 'uv_size': [tw, th]}
            c = {'origin': list(origin), 'size': list(size), 'uv': uv}
            if len(cube) > 4 and cube[4]: c['pivot'] = list(cube[4][0]); c['rotation'] = list(cube[4][1])
            out.append(c)
        if out: b['cubes'] = out
        self.bones.append(b)
    def save(self, name):
        geo = {'format_version': '1.12.0', 'minecraft:geometry': [{
            'description': {'identifier': 'geometry.' + self.ident, 'texture_width': self.tex, 'texture_height': self.tex,
                            'visible_bounds_width': 6, 'visible_bounds_height': 4, 'visible_bounds_offset': [0, 1.5, 0]},
            'bones': self.bones}]}
        json.dump(geo, open(ensure(P('geo', self.ident + '.geo.json')), 'w'), ensure_ascii=False, indent=1)
        self.img.save(ensure(P('textures', 'entity', name + '.png')))

# ---------------------------------------------------------------- художники граней
def flat(col, shade=True, noise=0.04, seed=1):
    def paint(fc, d, r):
        k = {'up': 1.12, 'down': 0.7, 'north': 1.0, 'south': 0.9, 'east': 0.86, 'west': 0.86}[fc] if shade else 1
        rnd = random.Random(seed + hash(fc) % 97)
        for x in range(r[0], r[2]):
            for y in range(r[1], r[3]):
                d.point((x, y), mul(col, k * (1 + rnd.uniform(-noise, noise))))
    return paint

def lace_trim(base, lace, where='bottom', width=0.22, seed=3):
    """Кружево: зубчатая светлая кайма с дырочками — по низу/верху боковых граней."""
    inner = flat(base, seed=seed)
    def paint(fc, d, r):
        inner(fc, d, r)
        if fc in ('up', 'down'): return
        x0, y0, x1, y1 = r; h = y1 - y0; bw = max(2, int(h * width))
        ys = range(y1 - bw, y1) if where == 'bottom' else range(y0, y0 + bw)
        for x in range(x0, x1):
            for y in ys:
                t = (y - (y1 - bw)) if where == 'bottom' else (y0 + bw - 1 - y)
                scallop = (x - x0) % 4
                if t >= bw - 1 and scallop in (0, 3): continue          # зубчики
                if (x - x0) % 4 == 2 and t == bw // 2: d.point((x, y), mul(base, 0.6)); continue  # дырочки
                d.point((x, y), lace)
    return paint

def stripes(base, stripe, every=4, seed=5):
    inner = flat(base, seed=seed)
    def paint(fc, d, r):
        inner(fc, d, r)
        if fc in ('up', 'down'): return
        for y in range(r[1], r[3]):
            if (y - r[1]) % every == 0:
                for x in range(r[0], r[2]): d.point((x, y), stripe)
    return paint

def anime_face(f, long_hair=False):
    """Лицо (faces.py) на северной грани головы; виски/затылок/макушка — волосы."""
    skin, hair = f['skin'], f['hair']
    skin_p, hair_p = flat(skin, noise=0.01), gradient_hair(mul(hair, 1.08), mul(hair, 0.85), seed=9)
    def paint(fc, d, r):
        x0, y0, x1, y1 = r
        if fc == 'north':
            st = os.environ.get('FACE_STYLE', 'ref') if f.get('sex') == 'f' else None   # v0.9.5: стиль лица девушек — «как на картинке» (FACE_STYLE=old — прежний)
            if st == 'old': st = None
            if st: import faces2; d._image.paste(faces2.face_image(f, x1 - x0, y1 - y0, st), (x0, y0)); return
            d._image.paste(faces.face_image(f, x1 - x0, y1 - y0), (x0, y0)); return
        if fc in ('up', 'south'): hair_p(fc, d, r); return
        skin_p(fc, d, r)
        if fc in ('east', 'west'):
            H = y1 - y0
            for x in range(x0, x1):
                t = (x - x0) / max(1, x1 - x0 - 1)
                if fc == 'east': t = 1 - t       # t=1 — у затылка
                cut = H if long_hair else int(H * (0.45 + 0.35 * t))
                for y in range(y0, y0 + cut): d.point((x, y), mul(hair, 0.95 + 0.08 * ((x - x0) % 4 == 0)))
    return paint

def gradient_hair(top, bottom, seed=11):
    def paint(fc, d, r):
        x0, y0, x1, y1 = r; rnd = random.Random(seed + hash(fc) % 31)
        for y in range(y0, y1):
            t = (y - y0) / max(1, (y1 - y0 - 1))
            for x in range(x0, x1):
                c = mix(top, bottom, t); strand = 1.0 + (0.1 if (x - x0) % 3 == 0 else 0) + rnd.uniform(-0.04, 0.04)
                d.point((x, y), mul(c, strand))
    return paint

def strands(top, tip, seed=7, ring=True):
    """Волосы: пряди-полоски, градиент к кончикам, блик-«нимб» в верхней трети, тёмные промежутки."""
    def paint(fc, d, r):
        x0, y0, x1, y1 = r; W, H = x1 - x0, y1 - y0; rnd = random.Random(seed + hash(fc) % 101)
        offs = [rnd.uniform(-0.05, 0.05) for _ in range(W + 2)]
        for x in range(x0, x1):
            k0 = 1 + offs[x - x0] + (-0.1 if (x - x0) % 5 == 0 else 0)
            for y in range(y0, y1):
                t = (y - y0) / max(1, H - 1) if fc not in ('up', 'down') else 0.15
                c = mix(top, tip, min(1, t ** 1.3))
                k = k0
                if ring and fc not in ('up', 'down') and abs(t - 0.22) < 0.05: k += 0.28 * (1 - abs(t - 0.22) / 0.05)
                d.point((x, y), mul(c, k))
    return paint

def membrane(base, vein, seed=3):
    """Перепонка крыла летучей мыши: прожилки веером, зубчатый прозрачный нижний край."""
    def paint(fc, d, r):
        x0, y0, x1, y1 = r; W, H = x1 - x0, y1 - y0
        if fc in ('up', 'down', 'east', 'west'):
            for x in range(x0, x1):
                for y in range(y0, y1): d.point((x, y), vein)
            return
        for x in range(x0, x1):
            u = (x - x0) / max(1, W - 1)
            uu = u if fc == 'north' else 1 - u
            edge = H * (0.62 + 0.38 * abs(math.sin(uu * math.pi * 3)) ** 0.6)   # три «пальца» — зубцы снизу
            for y in range(y0, y1):
                v = y - y0
                if v > edge: continue
                k = 0.85 + 0.25 * (1 - v / H)
                d.point((x, y), mul(base, k))
        for f in (0.0, 0.34, 0.67, 1.0):   # кости-прожилки
            xa = x0 + int(f * (W - 1)) if fc == 'north' else x1 - 1 - int(f * (W - 1))
            d.line([(x0 if fc == 'north' else x1 - 1, y0), (xa, y0 + int(H * (0.95 if f in (0.0, 1.0) else 0.98)) - 1)], fill=vein, width=1)
    return paint

def split_v(top_p, bot_p, frac, trim=None):
    """Грань делится по высоте: верх — top_p (например кожа), низ — bot_p; по линии раздела — кайма trim (кружево)."""
    def paint(fc, d, r):
        x0, y0, x1, y1 = r
        if fc == 'up': top_p(fc, d, r); return
        bot_p(fc, d, r)
        if fc == 'down': return
        cut = y0 + int((y1 - y0) * frac)
        if cut > y0: top_p(fc, d, (x0, y0, x1, cut))
        if trim:
            for x in range(x0, x1):
                for y in range(cut, min(y1, cut + max(1, (y1 - y0) // 14))):
                    if (x - x0) % 3 != 2 or y > cut: d.point((x, y), trim)
    return paint

def neckline(cloth, skin, edge, kind='sweet', front=0.45, back=0.3, side=0.4, scallop=True):
    """Вырез: 'sweet' — открытые плечи и «сердечко» спереди; 'v' — V-вырез с лацканами."""
    cl, sk = cloth, skin
    def paint(fc, d, r):
        x0, y0, x1, y1 = r; W, H = x1 - x0, y1 - y0
        cl(fc, d, r)
        if fc == 'down': return
        if fc == 'up':
            if kind == 'sweet': sk(fc, d, r)
            else: sk(fc, d, (x0 + int(W * 0.3), y0, x1 - int(W * 0.3), y1))
            return
        for x in range(x0, x1):
            u = (x - x0 + 0.5) / W
            if kind == 'v':
                if fc != 'north': continue
                cut = int(H * front * max(0, 1 - abs(u - 0.5) / 0.26))
            else:
                base = {'north': front, 'south': back}.get(fc, side)
                cut = int(H * (base + (0.08 * (1 - abs(math.sin(math.pi * 2 * u))) if fc == 'north' else 0)))
            if cut <= 0: continue
            sk(fc, d, (x, y0, x + 1, y0 + cut))
            ew = max(1, H // 10)
            for y in range(y0 + cut, min(y1, y0 + cut + ew)):
                if not scallop or (x - x0) % 4 != 3 or y > y0 + cut: d.point((x, y), edge)
            if kind == 'v':   # лацкан — вторая линия
                for y in range(y0 + cut + ew, min(y1, y0 + cut + 2 * ew)): d.point((x, y), mul(edge, 0.75))
    return paint

# ---------------------------------------------------------------- персонажи (1 единица = 1/16 блока, стоят на y = 0)
def humanoid(m, skin, face, torso, arm, leg, shoe, head_size=(9, 9, 8), fem=False, D=4, extra=None, bust=None, top=None):
    """Тело. Девушки: узкая талия, грудь, тонкие руки и ноги; парни: шире плечи и руки."""
    hw, hh, hd = head_size
    lw, aw = (2.6, 1.9) if fem else (3.0, 2.4)          # толщина ног и рук
    sw = 6.0 if fem else 7.0                             # ширина плеч
    m.bone('root', None, [0, 0, 0])
    m.bone('right_leg', 'root', [-1.5, 12, 0], [([-1.5 - lw / 2, 0, -lw / 2], [lw, 12, lw], leg, 2), ([-1.6 - lw / 2, 0, -lw / 2 - 0.3], [lw + 0.2, 2.5, lw + 0.6], shoe, 2)])
    m.bone('left_leg', 'root', [1.5, 12, 0], [([1.5 - lw / 2, 0, -lw / 2], [lw, 12, lw], leg, 2), ([1.4 - lw / 2, 0, -lw / 2 - 0.3], [lw + 0.2, 2.5, lw + 0.6], shoe, 2)])
    if fem:   # талия уже груди и бёдер
        body = [([-3.2, 12, -1.5], [6.4, 2, 3.0], torso, 3), ([-2.5, 14, -1.3], [5.0, 3, 2.6], torso, 3), ([-3.0, 17, -1.5], [6.0, 4.5, 3.0], top or torso, 3)]
    else:
        body = [([-sw / 2, 12, -1.7], [sw, 9.5, 3.4], torso, 3)]
    m.bone('body', 'root', [0, 12, 0], body)
    if fem:
        m.bone('chest', 'body', [0, 19.5, -1.5], [([-2.6, 17.4, -2.6], [5.2, 2.6, 1.2], bust or torso, 3)], rotation=[-12, 0, 0])
    m.bone('right_arm', 'body', [-sw / 2 - aw / 2, 20.5, 0], [([-sw / 2 - aw, 12, -aw / 2], [aw, 9, aw], arm, 2)])
    m.bone('left_arm', 'body', [sw / 2 + aw / 2, 20.5, 0], [([sw / 2, 12, -aw / 2], [aw, 9, aw], arm, 2)])
    m.bone('neck', 'body', [0, 21.5, 0], [([-0.9 if fem else -1.1, 21.5, -0.9], [1.8 if fem else 2.2, 1, 1.8], flat(skin), 2)])
    m.bone('head', 'body', [0, 22, 0], [([-hw / 2, 22.5, -hd / 2], [hw, hh, hd], face, 16)])
    if extra: extra(m)

def feather_paint(seed, kind='primary', glow=0.5):
    """Одно перо на грань-пластину: основание сверху, кончик снизу; зубчатые края, рваный кончик,
    тёмное опахало, стержень, багровая светящаяся кромка и зигзаг-прожилки (как трещины с жаром)."""
    def paint(fc, d, r):
        if fc not in ('north', 'south'): return
        x0, y0, x1, y1 = r; W, H = x1 - x0, y1 - y0
        S = 4; im = Image.new('RGBA', (W * S, H * S), (0, 0, 0, 0)); dr = ImageDraw.Draw(im)
        rnd = random.Random(seed * 7 + (fc == 'south'))
        cx, Wp, Hp = W * S / 2, W * S, H * S
        left, right = [], []
        n = 40
        for i in range(n + 1):
            t = i / n
            if kind == 'covert': w = (1 - max(0, t - 0.5) ** 2 / 0.25) ** 0.5 * min(1, t * 5 + 0.4)
            else: w = min(1, t / 0.08 + 0.35) ** 0.5 * max(0, 1 - t ** 2.2) ** 0.6
            saw = 0.0
            if kind != 'covert' and t > 0.3 and i % 3 == 0: saw = rnd.uniform(0.25, 0.55)    # зазубрины
            hw = w * Wp * 0.5
            left.append((cx - hw * (1 - saw * (i % 2 == 0)), t * Hp))
            right.append((cx + hw * (1 - saw * (i % 2 == 1)), t * Hp))
        if kind != 'covert':   # рваный кончик: 2–3 зубца
            tipy = Hp
            left[-1] = (cx - Wp * 0.12, tipy - Hp * 0.04); right[-1] = (cx + Wp * 0.10, tipy - Hp * rnd.uniform(0.05, 0.10))
            poly = left + [(cx, tipy)] + right[::-1]
        else: poly = left + right[::-1]
        dark, mid = (20, 12, 18, 255), (44, 26, 38, 255)
        mask = Image.new('L', im.size, 0); ImageDraw.Draw(mask).polygon(poly, fill=255)
        body = Image.new('RGBA', im.size, dark); bd = ImageDraw.Draw(body)
        for y in range(Hp):
            t = y / max(1, Hp - 1)
            c = tuple(int(mid[j] * (1 - t) + dark[j] * t) for j in range(3)) + (255,)
            bd.line([(0, y), (Wp, y)], fill=c)
        # бородки
        for k in range(int(Hp / (S * 1.6))):
            y = k * S * 1.6
            bd.line([(cx, y), (0, y + Wp * 0.5)], fill=(14, 8, 12, 255), width=max(1, S // 3))
            bd.line([(cx, y), (Wp, y + Wp * 0.5)], fill=(14, 8, 12, 255), width=max(1, S // 3))
        # стержень
        bd.line([(cx, 0), (cx, Hp * 0.85)], fill=(70, 40, 56, 255), width=max(1, S // 2 + 1))
        # светящиеся прожилки-зигзаги
        if rnd.random() < glow:
            x, y = cx + rnd.uniform(-0.2, 0.2) * Wp, rnd.uniform(0.15, 0.45) * Hp
            pts = [(x, y)]
            for _ in range(rnd.randint(3, 5)):
                x = max(Wp * 0.15, min(Wp * 0.85, x + rnd.choice((-1, 1)) * Wp * rnd.uniform(0.15, 0.3)))
                y = y + Hp * rnd.uniform(0.06, 0.12); pts.append((x, y))
            bd.line(pts, fill=(110, 10, 28, 255), width=S)
            bd.line(pts, fill=(210, 40, 60, 255), width=max(1, S // 2))
        im.alpha_composite(Image.composite(body, Image.new('RGBA', im.size, (0, 0, 0, 0)), mask))
        # кромка: багровая с одной стороны, к кончику ярче
        side = left if (seed % 2) else right
        for i in range(len(side) - 1):
            t = i / len(side)
            if t < 0.6: continue
            c = (int(90 + 100 * t * glow), int(12 + 14 * t), int(26 + 14 * t), 255)
            dr.line([side[i], side[i + 1]], fill=c, width=S)
        im2 = im.resize((W, H), Image.LANCZOS)
        d._image.paste(im2, (x0, y0))
    return paint

def katana_blade(fc, d, r):
    """Клинок: тёмная сталь с зеркальной кромкой и волной хамона; кровь густеет к острию, капли и потёки."""
    x0, y0, x1, y1 = r; W, H = x1 - x0, y1 - y0
    rnd = random.Random(77 + hash(fc) % 13)
    for x in range(x0, x1):
        u = (x - x0) / max(1, W - 1); u = 1 - u if fc == 'east' else u   # вдоль клинка (0 — у гарды, 1 — острие); грани развёрнуты по-разному
        for y in range(y0, y1):
            v = (y - y0) / max(1, H - 1)
            if fc in ('east', 'west'):
                hamon = 0.55 + 0.08 * math.sin(u * 40) + 0.04 * math.sin(u * 97)
                base = (205, 210, 222) if v < hamon - 0.05 else (120, 126, 140)   # светлая закалённая кромка сверху, тёмное тело
                if abs(v - hamon) < 0.05: base = (235, 238, 245)
                blood = max(0, min(1, (u - 0.35) / 0.65)) ** 1.4 * (0.6 + 0.4 * (1 - v))
                if rnd.random() < 0.06 * u: blood = min(1, blood + 0.5)
            else:
                base = (150, 156, 170) if fc == 'up' else (70, 74, 84)
                ul = v if fc == 'up' else 1 - v      # у верхней/нижней грани длина клинка — по второй оси
                blood = max(0, (ul - 0.4)) * 1.2 if fc == 'up' else 0
            c = tuple(int(base[i] * (1 - blood) + (120, 6, 18)[i] * blood) for i in range(3)) + (255,)
            d.point((x, y), c)
    if fc in ('east', 'west'):   # потёки
        for _ in range(6):
            sx = x0 + rnd.randint(W // 3, W - 2)
            for yy in range(y0 + rnd.randint(0, H // 3), y1): d.point((sx, yy), (95, 4, 14, 255))

def katana_grip(fc, d, r):
    """Рукоять: бордовая оплётка ромбами по чёрной коже ската."""
    x0, y0, x1, y1 = r
    for x in range(x0, x1):
        for y in range(y0, y1):
            k = ((x - x0) + (y - y0)) % 6, ((x - x0) - (y - y0)) % 6
            d.point((x, y), (140, 18, 32, 255) if 0 in k else (22, 14, 18, 255))

def build_katana(m):
    """Крупная катана в правой руке Кармиллы: рукоять в кулаке, золотая цуба, клинок ~1.5 блока вперёд-вниз."""
    hx, hy = -3.95, 12.6
    tilt = ([hx, hy, 0], [18, 0, 0])
    m.bone('katana', 'right_arm', [hx, hy, 0], [
        ([hx - 0.45, hy - 0.45, -1.2], [0.9, 0.9, 4.2], katana_grip, 8, tilt),                                   # рукоять
        ([hx - 0.5, hy - 0.5, 2.9], [1.0, 1.0, 0.5], flat(hexc('c9a64d'), noise=0.1), 8, tilt),                  # навершие
        ([hx - 1.2, hy - 1.0, -1.7], [2.4, 2.0, 0.45], flat(hexc('b8913a'), noise=0.12), 10, tilt),              # цуба
        ([hx - 0.55, hy - 0.6, -2.4], [1.1, 1.2, 0.7], flat(hexc('d8c070'), noise=0.06), 8, tilt),               # хабаки
        ([hx - 0.22, hy - 0.85, -26.4], [0.44, 1.7, 24.0], katana_blade, 8, tilt),                               # клинок
        ([hx - 0.15, hy - 0.45, -27.6], [0.3, 1.0, 1.25], katana_blade, 8, tilt),                               # кончик-киссаки (уже клинка)
    ])

def build_wings(m):
    """Объёмные распахнутые крылья: кость из сегментов, на ней три слоя перьев-пластин веером
    (маховые сзади, второстепенные посередине, кроющие спереди) + шипы-перья по верхней кромке.
    Каждое перо — отдельный куб со своим поворотом: z — веер, x — наклон вперёд/назад для объёма."""
    rnd = random.Random(5)
    # кость в «внешних» координатах (X наружу): лопатка -> локоть -> запястье -> кончик
    # высокое крыло: плечо круто вверх, запястье над головой, кисть наружу; перья свисают почти до земли
    ARM = [(1.0, 22.5), (4.5, 31.0), (9.5, 38.0), (15.0, 39.5)]
    seg = [(ARM[i], ARM[i + 1]) for i in range(3)]
    lens = [math.hypot(b[0] - a[0], b[1] - a[1]) for a, b in seg]; total = sum(lens)
    def at(s):
        L = s * total
        for (a, b), l in zip(seg, lens):
            if L <= l or (a, b) == seg[-1]:
                t = min(1, L / l); return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t)
            L -= l
    bone_p = flat(hexc('1a0f16'), noise=0.06)
    for sx, nm in ((-1, 'r'), (1, 'l')):
        cubes, tipc = [], []
        def feather(base, L, w, ang, z, kind, rx=0.0, glow=0.5, tip=False):
            bx, by = base[0] * sx, base[1]
            seed = rnd.randint(0, 10 ** 6)
            (tipc if tip else cubes).append(([bx - w / 2, by - L, z], [w, L, 0.12], feather_paint(seed, kind, glow), 7,
                          ([bx, by, z + 0.06], [round(rx, 1), 0, round(ang * sx, 1)])))
        def lay(s_):   # «естественный» наклон пера в точке кости
            if s_ < 0.38: return -3 + 4 * s_ / 0.38
            if s_ < 0.72: return 1 + 7 * (s_ - 0.38) / 0.34
            return 8 + 28 * ((s_ - 0.72) / 0.28) ** 1.1
        def hang(s_, tipy, w, z, kind, rx, glow, da=0.0):
            b_ = at(s_); ang = lay(s_) + da
            L = (b_[1] - tipy) / max(0.35, math.cos(math.radians(ang)))
            feather(b_, L, w, ang, z, kind, rx=rx, glow=glow)
        # маховые (кисть): расходятся «пальцами» наружу — образуют острый внешний угол крыла
        n = 12
        for k in range(n):
            f = k / (n - 1); s_ = 0.70 + 0.30 * f
            feather(at(s_), 30 - 15 * f + rnd.uniform(-1.2, 1.2), 3.3, 14 + 50 * f ** 1.15 + rnd.uniform(-3, 3), 7.25 + 0.04 * k, 'primary',
                    rx=rnd.uniform(-6, 10), glow=0.28, tip=True)
        # второстепенные (предплечье): длинные, почти отвесно — нижний край крыла у самой земли
        n = 11
        for k in range(n):
            f = k / (n - 1)
            hang(0.36 + 0.36 * f, 3 + 3 * (1 - f) + rnd.uniform(-1.2, 1.2), 3.3, 6.9 + 0.03 * k, 'primary', rnd.uniform(-4, 8), 0.2, 3 + 6 * f + rnd.uniform(-2, 2))
        # третьестепенные (плечо): у спины, короче — открывают силуэт талии
        n = 7
        for k in range(n):
            f = k / (n - 1)
            hang(0.02 + 0.36 * f, 14 - 4 * f + rnd.uniform(-1, 1), 3.0, 6.6 + 0.03 * k, 'primary', rnd.uniform(-4, 6), 0.15, rnd.uniform(-3, 3))
        # большие кроющие: средний ряд заострённых перьев поверх оснований маховых
        n = 17
        for k in range(n):
            f = k / (n - 1); s_ = f; b_ = at(s_)
            feather(b_, 8 + 4 * math.sin(math.pi * f) + rnd.uniform(-1, 1), 2.8, lay(s_) + rnd.uniform(-4, 4), 6.35 + 0.02 * k, 'primary',
                    rx=rnd.uniform(-10, -2), glow=0.15, tip=s_ >= 0.76)
        # малые кроющие: «чешуя» вдоль кости
        n = 19
        for k in range(n):
            f = k / (n - 1); s_ = f; b_ = at(s_)
            feather(b_, 4 + 1.5 * math.sin(math.pi * f) + rnd.uniform(-0.5, 0.5), 2.4, lay(s_) + rnd.uniform(-6, 6), 6.15 + 0.01 * k, 'covert',
                    rx=rnd.uniform(-14, -4), glow=0.06, tip=s_ >= 0.76)
        # ВЕРХ КРЫЛА — анатомически:
        # 1) кроющие переднего края: встопорщены вверх-наружу, длиннее к середине, два шахматных слоя
        n = 15
        for k in range(n):
            f = k / (n - 1); s_ = 0.12 + 0.62 * f
            feather(at(s_), 5 + 4 * math.sin(math.pi * f) + rnd.uniform(-1, 1), 2.8, 165 - 45 * f + rnd.uniform(-8, 8),
                    6.4 + 0.35 * (k % 2), 'primary', rx=rnd.uniform(-10, 6), glow=0.3)
        # 2) крылышко (алула) на запястье: пучок длинных перьев вверх-вперёд — самая высокая точка крыла
        for k, (ln, ang) in enumerate(((11, 150), (9.5, 132), (8, 116), (6, 102))):
            feather(at(0.74 + 0.025 * k), ln + rnd.uniform(-0.5, 0.5), 3.0, ang + rnd.uniform(-4, 4), 6.5 + 0.2 * k, 'primary',
                    rx=rnd.uniform(-6, 6), glow=0.45, tip=True)
        # 3) кроющие кисти: короче, веером наружу по верхней кромке кисти
        n = 6
        for k in range(n):
            f = k / (n - 1); s_ = 0.82 + 0.17 * f
            feather(at(s_), 6 - 2 * f + rnd.uniform(-0.6, 0.6), 2.6, 100 - 15 * f + rnd.uniform(-6, 6), 6.45 + 0.25 * (k % 2), 'primary',
                    rx=rnd.uniform(-8, 6), glow=0.3, tip=True)
        # 4) плечевые: у лопатки, торчат вверх за спиной
        for k, (ln, ang) in enumerate(((7, 182), (6, 172), (5, 192))):
            feather(at(0.02 + 0.04 * k), ln + rnd.uniform(-0.5, 0.5), 2.6, ang + rnd.uniform(-5, 5), 6.7 + 0.15 * k, 'primary', rx=rnd.uniform(-8, 4), glow=0.2)
        # 5) малые кроющие-«чешуя» поверх основания всех верхних перьев
        n = 14
        for k in range(n):
            f = k / (n - 1); s_ = 0.05 + 0.92 * f
            feather(at(s_), 3 + 1.5 * math.sin(math.pi * f) + rnd.uniform(-0.4, 0.4), 2.4, 160 - 70 * f + rnd.uniform(-10, 10),
                    6.2 + 0.02 * k, 'covert', rx=rnd.uniform(-12, 0), glow=0.08, tip=s_ >= 0.76)
        # кость: толстые сегменты
        for (a, b), l in zip(seg, lens):
            ax = a[0] * sx; ang = math.degrees(math.atan2(b[1] - a[1], b[0] - a[0])) * sx
            th = 1.6 if a == ARM[0] else 1.2
            x0 = ax if sx > 0 else ax - l
            piv_ang = ang if sx > 0 else ang + 180 * 0  # поворот вокруг точки a
            dst = tipc if a == ARM[2] else cubes
            if sx > 0: dst.append(([ax, a[1] - th / 2, 6.0], [l + 0.4, th, th], bone_p, 4, ([ax, a[1], 6.0 + th / 2], [0, 0, round(ang, 1)])))
            else:      dst.append(([ax - l - 0.4, a[1] - th / 2, 6.0], [l + 0.4, th, th], bone_p, 4, ([ax, a[1], 6.0 + th / 2], [0, 0, round(ang, 1)])))
        # чуть меньше: масштаб вокруг корня крыла (повороты не меняются)
        SC, O = 0.9, (1.0 * sx, 22.5)
        def sc(p): return [O[0] + (p[0] - O[0]) * SC, O[1] + (p[1] - O[1]) * SC, p[2]]
        def scale(c):
            c = list(c); c[0] = sc(c[0]); c[1] = [c[1][0] * SC, c[1][1] * SC, c[1][2]]
            if len(c) > 4: c[4] = (sc(c[4][0]), c[4][1])
            return tuple(c)
        cubes, tipc = [scale(c) for c in cubes], [scale(c) for c in tipc]
        m.bone('wing_' + nm, 'body', [1.0 * sx, 22.5, 6.6], cubes)
        # кисть — отдельная кость: в взмахе догоняет с запаздыванием (волна по крылу)
        m.bone('wing_' + nm + '_tip', 'wing_' + nm, sc([ARM[2][0] * sx, ARM[2][1], 6.6]), tipc)


def carmilla():
    m = Model('carmilla', 2048)
    skin = hexc('f4e3e0'); hair_top, hair_bot = hexc('1b1420'), hexc('3a1426')
    black, crimson, lace = hexc('1a1218'), hexc('8c1f2b'), hexc('efe6e2')
    face = anime_face(dict(skin=skin, hair=hair_top, eye=hexc('d01830'), sex='f', expr='sly', bangs='hime', fangs=True,
                           brow=hexc('2a1a22'), lash=hexc('140a10'), blush_c=(220, 90, 120, 90)), long_hair=True)
    def extra(m):
        hair = strands(mul(hair_top, 1.25), hair_bot, seed=11)
        # объёмные волосы: «шапка» на макушке, пышный затылок, расширяющаяся середина, кончики-пряди веером
        m.bone('hair', 'head', [0, 31, 4], [([-5.1, 29.4, -4.9], [10.2, 2.8, 10.2], hair, 4),
                                             ([-5.4, 20.5, 2.6], [10.8, 10, 2.6], hair, 4),
                                             ([-5.7, 21.5, -3.0], [1.4, 9, 6.2], hair, 4), ([4.3, 21.5, -3.0], [1.4, 9, 6.2], hair, 4)])
        m.bone('hair_mid', 'hair', [0, 21, 4.4], [([-6.0, 13.5, 3.0], [12, 7.5, 3.0], hair, 4)])
        tips = []
        for k, (x, ln, ang) in enumerate(((-5.9, 6, 8), (-3.6, 7.5, 3), (-1.2, 8, 0), (1.2, 7.5, -3), (3.6, 6.5, -6))):
            tips.append(([x, 13.6 - ln, 3.2 + (k % 2) * 0.4], [2.6, ln, 2.4 - (k % 2) * 0.4], hair, 4, ([x + 1.3, 13.6, 4.4], [0, 0, ang])))
        m.bone('hair_tip', 'hair_mid', [0, 13.6, 4.4], tips)
        # пряди у лица — толстые, с сужением к концу
        m.bone('lock_r', 'head', [-4.6, 27, -3.9], [([-5.8, 19, -4.9], [1.8, 9, 2.4], hair, 4), ([-5.5, 16.5, -4.6], [1.2, 2.5, 1.8], hair, 4)])
        m.bone('lock_l', 'head', [4.6, 27, -3.9], [([4.0, 19, -4.9], [1.8, 9, 2.4], hair, 4), ([4.3, 16.5, -4.6], [1.2, 2.5, 1.8], hair, 4)])
        # бант на затылке
        m.bone('bow', 'head', [0, 28, 5], [([-0.8, 27.2, 5.0], [1.6, 1.6, 1.0], flat(crimson, noise=0.08), 4),
                                           ([-3.6, 26.6, 5.0], [2.8, 2.8, 0.8], flat(crimson, noise=0.08), 4, ([-0.8, 28, 5.4], [0, 0, 18])),
                                           ([0.8, 26.6, 5.0], [2.8, 2.8, 0.8], flat(crimson, noise=0.08), 4, ([0.8, 28, 5.4], [0, 0, -18]))])
        # крылья (форма по референсу): два слоя-пластины с HD-текстурой перьев; корень у лопаток, раскрыты в стороны
        build_wings(m)
        build_katana(m)
        # пояс с серебряной пряжкой
        m.bone('belt', 'body', [0, 14, 0], [([-2.7, 14.0, -1.5], [5.4, 0.9, 3.0], flat(hexc('1a1012')), 4), ([-0.7, 13.8, -1.75], [1.4, 1.3, 0.3], flat(hexc('c8ccd8'), noise=0.1), 6)])
        # туфли на каблуке
        m.bone('heel_r', 'right_leg', [-1.5, 12, 0], [([-2.2, 0, 0.8], [1.0, 1.6, 0.9], flat(hexc('1a0a0e')), 4), ([-2.9, 0, -2.4], [2.8, 1.0, 1.4], flat(hexc('3a0e16')), 4)])
        m.bone('heel_l', 'left_leg', [1.5, 12, 0], [([1.2, 0, 0.8], [1.0, 1.6, 0.9], flat(hexc('1a0a0e')), 4), ([0.1, 0, -2.4], [2.8, 1.0, 1.4], flat(hexc('3a0e16')), 4)])
        # заколка-роза
        m.bone('rose', 'head', [3, 30, 0], [([3, 30, -2], [2, 2, 2], flat(crimson, noise=0.1), 3)])
        # юбка с кружевом
        m.bone('skirt', 'body', [0, 13, 0], [([-4.6, 5, -3], [9.2, 8, 6], lace_trim(black, lace, 'bottom', 0.18), 3)])
        # корсет с красной шнуровкой
        m.bone('corset', 'body', [0, 13, 0], [([-2.6, 13.5, -1.6], [5.2, 4, 0.4], stripes(crimson, black, 3), 3)])
        # чокер
        m.bone('choker', 'neck', [0, 21.5, 0], [([-1.2, 21.4, -1.2], [2.4, 0.6, 2.4], flat(black), 3)])
        # плащ
        m.bone('cape', 'body', [0, 21, 2], [([-5, 13, 1.7], [10, 8, 0.6], flat(crimson), 2)])
        m.bone('cape_low', 'cape', [0, 13, 2], [([-5.4, 4, 1.7], [10.8, 9, 0.6], lace_trim(crimson, black, 'bottom', 0.12), 2)])
    sk = flat(skin, noise=0.01)
    # открытые плечи и вырез «сердечком» с кружевом; длинные кружевные перчатки от локтя
    humanoid(m, skin, face, flat(black), split_v(sk, lace_trim(black, lace, 'bottom', 0.12), 0.42, trim=lace),
             flat(hexc('1f1a22')), flat(hexc('2a0f14')), fem=True,
             top=neckline(flat(black), sk, lace, 'sweet', front=0.55, back=0.45, side=0.5),
             bust=neckline(flat(black), sk, lace, 'sweet', front=0.16, back=0, side=0.1), extra=extra)
    m.save('carmilla')

def eleonora():
    m = Model('eleonora')
    skin = hexc('f6e6d6'); hair = hexc('e3c27a'); navy, ivory, gold = hexc('1f2a44'), hexc('e9e2d0'), hexc('c9a64d')
    face = anime_face(dict(skin=skin, hair=hair, eye=hexc('3f7fd0'), sex='f', expr='stern', bangs='swept',
                           brow=hexc('a08a60'), lash=hexc('3a2a22'), blush_c=(240, 140, 150, 70)), long_hair=True)
    def extra(m):
        hp = strands(mul(hair, 1.15), mul(hair, 0.78), seed=21)
        m.bone('hair', 'head', [0, 31, 4], [([-5.1, 29.2, -4.9], [10.2, 2.9, 10.2], hp, 4), ([-5.2, 23.5, 2.8], [10.4, 7, 2.2], hp, 4),
                                             ([-5.6, 23, -3.0], [1.3, 7, 6.2], hp, 4), ([4.3, 23, -3.0], [1.3, 7, 6.2], hp, 4)])
        m.bone('ponytail', 'head', [0, 28, 4.8], [([-2.0, 20.5, 4.3], [4, 8, 3.6], hp, 4), ([-1.1, 27.4, 4.2], [2.2, 1.2, 1.8], flat(gold, noise=0.1), 6)], rotation=[10, 0, 0])
        m.bone('ponytail_tip', 'ponytail', [0, 20.5, 6.0], [([-1.6, 13.5, 4.6], [3.2, 7, 2.8], hp, 4), ([-1.0, 11.2, 4.9], [2.0, 2.4, 2.0], hp, 4)])
        m.bone('lock_r', 'head', [-4.5, 27, -3.9], [([-5.6, 21, -4.8], [1.6, 7, 2.0], hp, 4)])
        m.bone('lock_l', 'head', [4.5, 27, -3.9], [([4.0, 21, -4.8], [1.6, 7, 2.0], hp, 4)])
        # арбалет за спиной, наискосок
        wood, steel = flat(hexc('5a3a22'), noise=0.08), flat(hexc('9aa2ae'), noise=0.06)
        m.bone('crossbow', 'body', [0, 17, 2.4], [([-0.6, 11, 2.6], [1.2, 11, 1.2], wood, 4, ([0, 17, 3.2], [0, 0, 32])),
                                                   ([-5, 19.6, 2.4], [10, 0.8, 0.8], steel, 4, ([0, 20, 2.8], [0, 0, 32])),
                                                   ([-4.6, 19.9, 3.2], [9.2, 0.2, 0.2], flat(hexc('e8e2d0')), 4, ([0, 20, 3.3], [0, 0, 32]))])
        # перевязь с флаконами святой воды
        m.bone('bandolier', 'body', [0, 17, -1.6], [([-0.6, 13, -1.85], [1.2, 9.6, 0.3], flat(hexc('3a2a1e')), 4, ([0, 17.5, -1.7], [0, 0, -38]))]
               + [([x, y, -2.35], [0.8, 1.3, 0.6], flat(hexc('7ac8f0'), noise=0.15), 6) for (x, y) in ((-1.9, 19.0), (-0.6, 17.4), (0.7, 15.8))])
        # пояс с серебряным колом и подсумком
        m.bone('belt', 'body', [0, 14, 0], [([-2.8, 13.6, -1.55], [5.6, 1.0, 3.1], flat(hexc('3a2a1e')), 4), ([-0.6, 13.5, -1.8], [1.2, 1.2, 0.3], flat(gold, noise=0.1), 6),
                                            ([2.6, 10.5, -0.8], [0.6, 4, 0.6], flat(hexc('d8dce6'), noise=0.05), 6), ([-3.4, 11.8, -1.2], [1.4, 1.8, 1.6], flat(hexc('4a3624')), 4)])
        # высокие сапоги с отворотом
        m.bone('boot_r', 'right_leg', [-1.5, 12, 0], [([-2.95, 4.5, -1.5], [2.9, 1.0, 2.9], flat(hexc('4a3220')), 4)])
        m.bone('boot_l', 'left_leg', [1.5, 12, 0], [([0.05, 4.5, -1.5], [2.9, 1.0, 2.9], flat(hexc('4a3220')), 4)])
        # перо на шляпе
        m.bone('feather', 'hat', [3, 34, 0], [([3.5, 33, -0.2], [0.4, 5, 1.6], flat(hexc('e8e2d0'), noise=0.06), 6, ([3.7, 33, 0.6], [-25, 0, -20]))])
        m.bone('hat', 'head', [0, 32, 0], [([-7.2, 32.0, -6.2], [14.4, 0.8, 12.4], flat(navy), 3), ([-4.6, 32.8, -4.1], [9.2, 3.2, 8.2], lace_trim(navy, gold, 'bottom', 0.25), 3)])
        m.bone('coat', 'body', [0, 13, 0], [([-3.8, 6, -2.0], [7.6, 7, 4.0], flat(navy), 3)])
        m.bone('coat_low', 'coat', [0, 6, 0], [([-4.2, 3, -2.2], [8.4, 3, 4.4], lace_trim(navy, gold, 'bottom', 0.3), 3)])
        m.bone('cross', 'body', [0, 18, -2], [([-0.5, 15.0, -1.7], [1, 2.2, 0.3], flat(gold, noise=0.1), 4), ([-1, 16.1, -1.7], [2, 0.5, 0.3], flat(gold, noise=0.1), 4)])
        m.bone('collar', 'body', [0, 21, 0], [([-3.2, 20.6, -0.4], [6.4, 1.2, 2.2], flat(ivory), 3)])   # отложной воротник сзади
    sk = flat(skin, noise=0.01)
    # расстёгнутый ворот (V-вырез с золотыми лацканами), рукава закатаны до локтя
    humanoid(m, skin, face, flat(navy), split_v(flat(navy), sk, 0.56, trim=ivory),
             flat(hexc('2b2b30')), flat(hexc('3a2a1e')), fem=True,
             top=neckline(flat(navy), sk, gold, 'v', front=0.95, scallop=False),
             bust=neckline(flat(navy), sk, gold, 'v', front=0.5, scallop=False), extra=extra)
    m.save('eleonora')

TOURISTS = [  # (имя, куртка, штаны, волосы, глаза, шапка)
    ('mechanic', hexc('3b6fb0'), hexc('2b3f66'), hexc('5a3b24'), hexc('6b4a2a'), hexc('d9a441')),
    ('photographer', hexc('e0823a'), hexc('3a3a40'), hexc('2a1d14'), hexc('3b6b4a'), None),
    ('chemist', hexc('5fae4a'), hexc('d8d4c8'), hexc('b0743a'), hexc('4a5ab0'), None),
    ('tracker', hexc('c86ab8'), hexc('3c4a36'), hexc('202020'), hexc('8a4a2a'), hexc('4a5a3a')),
]

TFACE = [  # механик — весёлый, фотограф — мягкая улыбка, химик — спокойный в очках, следопыт — суровый со шрамом
    dict(expr='grin', bangs='messy', freckles=True),
    dict(expr='soft', bangs='swept'),
    dict(expr='calm', bangs='short', glasses=True),
    dict(expr='grim', bangs='messy', scar=True),
]

def tourist(i):
    name, jacket, pants, hair, eye, cap = TOURISTS[i]
    m = Model('tourist')
    skin = hexc(['f1d6c0', 'e8c4a4', 'f6e2d2', 'd8b090'][i])
    face = anime_face(dict(skin=skin, hair=hair, eye=eye, sex='m', blush_c=(230, 130, 130, 60), **TFACE[i]))
    def extra(m):
        m.bone('backpack', 'body', [0, 18, 2], [([-2.6, 13, 1.6], [5.2, 6.5, 2.4], flat(mul(pants, 1.2)), 2)])
        hp = strands(mul(hair, 1.2), mul(hair, 0.85), seed=31 + i, ring=False)
        m.bone('hair', 'head', [0, 30, 0], [([-5.0, 29.3, -4.8], [10, 2.8, 9.8], hp, 4), ([-5.3, 25.5, -2.5], [1.0, 4.5, 6.5], hp, 4), ([4.3, 25.5, -2.5], [1.0, 4.5, 6.5], hp, 4)]
                 + [([x, 31.4, z], [2.2, 1.6, 2.2], hp, 4, ([x + 1.1, 31.4, z + 1.1], [rx, 0, rz])) for (x, z, rx, rz) in ((-3.6, -4.2, -20, 10), (-1.0, -4.6, -25, 0), (1.6, -4.3, -20, -12), (-2.4, -1.2, 0, 15), (1.0, 0.8, 10, -15))])
        m.bone('hair_back', 'head', [0, 30, 4], [([-4.8, 23.5, 3.6], [9.6, 6.5, 1.6], hp, 4)])
        m.bone('tuft', 'hair', [0, 31.5, 0], [([-0.9, 31.8, -1.5], [1.8, 2.4, 2.6], hp, 4)], rotation=[-15, 0, 18])
        # кепка есть в геометрии у всех (одна модель на четверых), у кого её нет — грани прозрачные
        capp = flat(cap) if cap is not None else (lambda fc, d, r: None)
        m.bone('cap', 'head', [0, 32, 0], [([-5.2, 31.9, -5.0], [10.4, 1.8, 10.2], capp, 3), ([-3.6, 31.9, -7.6], [7.2, 0.6, 2.8], capp, 3)])
    humanoid(m, skin, face, stripes(jacket, mul(jacket, 0.82), 6, seed=i), flat(jacket), flat(pants), flat(hexc('2a2a2a')), extra=extra)
    m.save('tourist_%d' % i)
    return m

# ---------------------------------------------------------------- анимации (общие кости)
def anim():
    def loop(len_, bones): return {'loop': True, 'animation_length': len_, 'bones': bones}
    def swing(a, period, phase=0): return {str(round(t, 3)): [round(a * math.sin(2 * math.pi * (t / period) + phase), 2), 0, 0] for t in [period * k / 8 for k in range(9)]}
    def wave(a, period, phase=0, axis=0, base=(0, 0, 0)):
        out = {}
        for k in range(9):
            t = period * k / 8; v = list(base); v[axis] += round(a * math.sin(2 * math.pi * k / 8 + phase), 2); out[str(round(t, 3))] = v
        return {'rotation': out}
    def wing_flap(A, period):
        """Взмах волной: плечо идёт первым (вниз-вверх по z + лёгкий отвод назад по y),
        кисть с маховыми догоняет с запаздыванием и большей амплитудой — кончики «хлещут»;
        на опускании крыло чуть подбирается (асимметрия: вниз быстро, вверх плавно)."""
        def keys(a, phase, back):
            out = {}
            for k in range(17):
                t = period * k / 16; ph = 2 * math.pi * k / 16 + phase
                v = a * (math.sin(ph) + 0.25 * math.sin(2 * ph))
                out[str(round(t, 3))] = [round(abs(v) * 0.08, 2), round(v * back, 2), round(v, 2)]
            return {'rotation': out}
        return {'wing_r': keys(A, 0, 0.3), 'wing_l': keys(-A, 0, 0.3),
                'wing_r_tip': keys(A * 0.75, -1.0, 0.15), 'wing_l_tip': keys(-A * 0.75, -1.0, 0.15)}
    def flow(period, lift, amp, side):
        """Волосы/плащ: базовый подъём назад lift, колыхание amp, каждый следующий сегмент — с запаздыванием."""
        return {'hair': wave(amp, period, 0, 0, (lift * 0.5, 0, 0)), 'hair_mid': wave(amp * 1.2, period, -0.8, 0, (lift * 0.5, 0, 0)),
                'hair_tip': wave(amp * 1.5, period, -1.6, 0, (lift * 0.4, 0, 0)),
                'hair_back': wave(amp * 0.6, period, 0, 0, (lift * 0.4, 0, 0)), 'tuft': wave(amp * 1.5, period, 0.5, 2),
                'ponytail': wave(side, period, 0, 2, (lift, 0, 0)), 'ponytail_tip': wave(side * 1.4, period, -1.0, 2, (lift * 0.6, 0, 0)),
                'lock_r': wave(amp * 0.8, period, 0.3, 2, (lift * 0.3, 0, -3)), 'lock_l': wave(amp * 0.8, period, 0.9, 2, (lift * 0.3, 0, 3)),
                'cape': wave(amp, period, 0, 0, (lift, 0, 0)), 'cape_low': wave(amp * 1.4, period, -1.0, 0, (lift * 0.6, 0, 0)),
                'coat_low': wave(amp * 0.5, period, -0.5, 0, (lift * 0.3, 0, 0)), 'skirt': wave(amp * 0.4, period, 0, 0),
                # крылья: в покое сложены и чуть дышат; на бегу раскрываются и машут
                # (в покое сложены за спину; чем быстрее движение — тем шире раскрыты и сильнее машут)
                # распахнутые крылья машут вверх-вниз (ось z), чуть подаваясь назад (ось y)
                **wing_flap(amp * 2.2 + lift * 0.6, period),
                }
    walk = loop(0.8, {**flow(0.8, 12, 4, 6), 'right_leg': {'rotation': swing(28, 0.8)}, 'left_leg': {'rotation': swing(28, 0.8, math.pi)},
                     'right_arm': {'rotation': swing(22, 0.8, math.pi)}, 'left_arm': {'rotation': swing(22, 0.8)}})
    run = loop(0.5, {**flow(0.5, 35, 6, 8), 'right_leg': {'rotation': swing(45, 0.5)}, 'left_leg': {'rotation': swing(45, 0.5, math.pi)},
                    'right_arm': {'rotation': swing(40, 0.5, math.pi)}, 'left_arm': {'rotation': swing(40, 0.5)},
                    'body': {'rotation': {'0': [8, 0, 0]}}})
    idle = loop(3.0, {**flow(3.0, 2, 2.5, 3), 'body': {'scale': {'0': [1, 1, 1], '1.5': [1, 1.012, 1], '3': [1, 1, 1]}}, 'head': {'rotation': {'0': [0, 0, 0], '1.5': [2, 3, 0], '3': [0, 0, 0]}},
                     'right_arm': {'rotation': {'0': [0, 0, 3]}}, 'left_arm': {'rotation': {'0': [0, 0, -3]}}})
    sneak = loop(1.2, {**flow(1.2, 6, 2, 4), 'body': {'rotation': {'0': [22, 0, 0]}}, 'head': {'rotation': {'0': [-15, 0, 0]}},
                      'right_leg': {'rotation': swing(16, 1.2)}, 'left_leg': {'rotation': swing(16, 1.2, math.pi)},
                      'right_arm': {'rotation': {'0': [-30, 0, 10]}}, 'left_arm': {'rotation': {'0': [-30, 0, -10]}}})
    stun = loop(0.6, {**flow(0.6, 4, 6, 8), 'head': {'rotation': {'0': [-10, -15, 0], '0.3': [-10, 15, 0], '0.6': [-10, -15, 0]}},
                     'right_arm': {'rotation': {'0': [-150, 0, 20]}}, 'left_arm': {'rotation': {'0': [-150, 0, -20]}}, 'body': {'rotation': {'0': [-8, 0, 0]}}})
    down = loop(2.0, {**flow(2.0, 0, 1, 1), 'root': {'rotation': {'0': [-85, 0, 0]}, 'position': {'0': [0, 2, 0]}}, 'right_arm': {'rotation': {'0': [-20, 0, 30], '1': [-40, 0, 30], '2': [-20, 0, 30]}}})
    cast = loop(0.8, {**flow(0.8, 20, 7, 8), 'right_arm': {'rotation': {'0': [-80, 0, 0], '0.4': [-95, 0, 0], '0.8': [-80, 0, 0]}}, 'left_arm': {'rotation': {'0': [-60, 0, 0]}}, 'body': {'rotation': {'0': [12, 0, 0]}}})
    # на колене: помогает упавшему / возится с дверью — руки вперёд, «давит» на рану
    kneel = loop(1.0, {**flow(1.0, 3, 2, 3), 'root': {'position': {'0': [0, -5, 0]}}, 'body': {'rotation': {'0': [18, 0, 0]}},
                       'right_leg': {'rotation': {'0': [-90, 0, 0]}, 'position': {'0': [0, 0, -2]}}, 'left_leg': {'rotation': {'0': [0, 0, 0]}, 'position': {'0': [0, 0, 3]}},
                       'right_arm': {'rotation': {'0': [-60, 0, 8], '0.5': [-70, 0, 8], '1': [-60, 0, 8]}}, 'left_arm': {'rotation': {'0': [-55, 0, -8], '0.5': [-65, 0, -8], '1': [-55, 0, -8]}}})
    # ползком (укушенный ползёт)
    crawl = loop(1.4, {**flow(1.4, 0, 1, 1), 'root': {'rotation': {'0': [-85, 0, 0]}, 'position': {'0': [0, 2, 0]}},
                       'right_arm': {'rotation': swing(35, 1.4)}, 'left_arm': {'rotation': swing(35, 1.4, math.pi)},
                       'right_leg': {'rotation': swing(12, 1.4, math.pi)}, 'left_leg': {'rotation': swing(12, 1.4)}})
    # v0.9.2: лезет по лестнице — руки поочерёдно перехватывают перекладины над головой, ноги переступают,
    # корпус прижат к лестнице, голова чуть запрокинута вверх; лёгкий рывок вверх на каждом перехвате
    def climb_keys(a0, a1, period, phase):
        out = {}
        for k in range(9):
            t = period * k / 8; v = 0.5 + 0.5 * math.sin(2 * math.pi * k / 8 + phase)
            out[str(round(t, 3))] = [round(a0 + (a1 - a0) * v, 1), 0, 0]
        return out
    climb = loop(0.9, {**flow(0.9, 4, 2, 3),
                       'body': {'rotation': {'0': [-8, 0, 0]}},
                       'head': {'rotation': {'0': [-18, 0, 0]}},
                       'right_arm': {'rotation': climb_keys(-175, -120, 0.9, 0)}, 'left_arm': {'rotation': climb_keys(-175, -120, 0.9, math.pi)},
                       'right_leg': {'rotation': climb_keys(-55, -5, 0.9, math.pi)}, 'left_leg': {'rotation': climb_keys(-55, -5, 0.9, 0)},
                       'root': {'position': {str(round(0.9 * k / 8, 3)): [0, round(0.6 * abs(math.sin(2 * math.pi * k / 8)), 2), 0] for k in range(9)}}})
    # удар катаной: замах над плечом → рубящий удар наискосок вниз; корпус доворачивается, крылья вздрагивают
    def once(len_, bones): return {'loop': 'hold_on_last_frame', 'animation_length': len_, 'bones': bones}
    slash = once(0.7, {**flow(0.7, 14, 3, 4),
                       'right_arm': {'rotation': {'0': [-20, 0, 10], '0.18': [-165, 25, 45], '0.34': [-60, -20, -35], '0.5': [-25, -10, -15], '0.7': [-20, 0, 5]}},
                       'body': {'rotation': {'0': [0, 0, 0], '0.18': [-6, 18, 0], '0.34': [10, -22, 0], '0.7': [2, -4, 0]}},
                       'head': {'rotation': {'0.18': [-6, 10, 0], '0.34': [8, -12, 0], '0.7': [0, 0, 0]}},
                       'left_arm': {'rotation': {'0.18': [-20, 0, -25], '0.34': [-35, 0, -10], '0.7': [-5, 0, 0]}},
                       'wing_r': {'rotation': {'0': [0, 0, 0], '0.18': [0, 6, 18], '0.34': [0, -4, -10], '0.7': [0, 0, 0]}},
                       'wing_l': {'rotation': {'0': [0, 0, 0], '0.18': [0, -6, -18], '0.34': [0, 4, 10], '0.7': [0, 0, 0]}}})
    # пронзание: рука отводится назад, клинок горизонтально — резкий выпад вперёд, корпус в наклоне; держит и выдёргивает
    pierce = once(0.9, {**flow(0.9, 10, 2, 3),
                        'right_arm': {'rotation': {'0': [-20, 0, 0], '0.22': [-70, 30, 25], '0.36': [-92, -5, 0], '0.62': [-92, -5, 0], '0.78': [-60, 10, 10], '0.9': [-25, 0, 0]}},
                        'body': {'rotation': {'0': [0, 0, 0], '0.22': [-8, 20, 0], '0.36': [18, -8, 0], '0.62': [18, -8, 0], '0.9': [2, 0, 0]}},
                        'root': {'position': {'0': [0, 0, 0], '0.22': [0, 0, 1.5], '0.36': [0, -1.0, -3.5], '0.62': [0, -1.0, -3.5], '0.9': [0, 0, 0]}},
                        'right_leg': {'rotation': {'0.36': [-30, 0, 0], '0.62': [-30, 0, 0], '0.9': [0, 0, 0]}},
                        'left_leg': {'rotation': {'0.36': [25, 0, 0], '0.62': [25, 0, 0], '0.9': [0, 0, 0]}},
                        'left_arm': {'rotation': {'0.36': [-10, 0, -40], '0.62': [-10, 0, -40], '0.9': [0, 0, 0]}},
                        'wing_r': {'rotation': {'0.22': [0, 0, 20], '0.36': [0, 0, -12], '0.9': [0, 0, 0]}},
                        'wing_l': {'rotation': {'0.22': [0, 0, -20], '0.36': [0, 0, 12], '0.9': [0, 0, 0]}}})
    data = {'format_version': '1.8.0', 'animations': {'idle': idle, 'walk': walk, 'run': run, 'sneak': sneak, 'stun': stun, 'down': down, 'cast': cast, 'kneel': kneel, 'crawl': crawl, 'climb': climb, 'slash': slash, 'pierce': pierce}}
    json.dump(data, open(ensure(P('animations', 'actor.animation.json')), 'w'), ensure_ascii=False, indent=1)

# ---------------------------------------------------------------- иконки предметов 16×16
class _SD:
    """Рисование в координатах 16×16 на холсте 256×256 (потом уменьшаем до 64 — гладкие HD-иконки)."""
    K = 16
    def __init__(self, d): self.d = d
    def _b(self, b): return [v * self.K + (self.K - 1 if i >= 2 else 0) for i, v in enumerate(b)] if len(b) == 4 else [v * self.K for v in b]
    def _p(self, pts): return [(x * self.K + self.K / 2, y * self.K + self.K / 2) for x, y in pts]
    def rectangle(self, b, fill=None, outline=None): self.d.rounded_rectangle(self._b(b), radius=self.K * 0.6, fill=fill, outline=outline, width=self.K)
    def ellipse(self, b, fill=None, outline=None, width=1): self.d.ellipse(self._b(b), fill=fill, outline=outline, width=width * self.K)
    def line(self, b, fill=None, width=1):
        pts = [(b[0], b[1]), (b[2], b[3])] if len(b) == 4 else b
        self.d.line(self._p(pts), fill=fill, width=int(width * self.K * 1.1), joint='curve')
        r = width * self.K * 0.55
        for x, y in self._p(pts): self.d.ellipse([x - r, y - r, x + r, y + r], fill=fill)
    def polygon(self, pts, fill=None, outline=None): self.d.polygon(self._p(pts), fill=fill, outline=outline)
    def arc(self, b, a0, a1, fill=None, width=1): self.d.arc(self._b(b), a0, a1, fill=fill, width=width * self.K)
    def point(self, xy, fill=None): x, y = xy; self.d.ellipse([x * self.K + 3, y * self.K + 3, x * self.K + self.K - 3, y * self.K + self.K - 3], fill=fill)

def icons():
    ids = ['wheel', 'jack', 'battery', 'canister', 'paddle', 'cutters', 'fuse', 'crowbar', 'ignition_key', 'spark_plug', 'antenna', 'fuel_hose', 'canister_full', 'flashlight', 'iron_key', 'brass_key', 'hammer', 'battery_cell']
    for idn in ids:
        big = Image.new('RGBA', (256, 256), (0, 0, 0, 0)); d = _SD(ImageDraw.Draw(big))
        if idn == 'wheel':
            d.ellipse([1, 1, 14, 14], fill=hexc('202024')); d.ellipse([4, 4, 11, 11], fill=hexc('9aa0a8')); d.ellipse([6, 6, 9, 9], fill=hexc('505560'))
        elif idn == 'jack':
            d.rectangle([2, 12, 13, 14], fill=hexc('b02a2a')); d.polygon([(4, 12), (8, 4), (12, 12)], outline=hexc('d04040')); d.rectangle([6, 2, 10, 4], fill=hexc('808890'))
        elif idn == 'battery':
            d.rectangle([2, 5, 13, 14], fill=hexc('2a2a30')); d.rectangle([2, 5, 13, 7], fill=hexc('3c8c4c')); d.rectangle([4, 3, 5, 4], fill=hexc('c0c0c0')); d.rectangle([10, 3, 11, 4], fill=hexc('c03030'))
        elif idn in ('canister', 'canister_full'):
            d.rectangle([3, 3, 12, 14], fill=hexc('c0302a' if idn == 'canister_full' else '7a2a26')); d.rectangle([9, 1, 11, 3], fill=hexc('303030')); d.line([4, 6, 11, 11], fill=hexc('e0b040'))
        elif idn == 'paddle':
            d.line([3, 13, 11, 5], fill=hexc('8a6033'), width=2); d.ellipse([9, 1, 14, 7], fill=hexc('a87a44'))
        elif idn == 'cutters':
            d.line([3, 3, 10, 10], fill=hexc('c03030'), width=2); d.line([3, 10, 10, 3], fill=hexc('3050c0'), width=2); d.polygon([(9, 9), (14, 14), (11, 9)], fill=hexc('a0a8b0'))
        elif idn == 'fuse':
            d.rectangle([5, 2, 10, 13], fill=hexc('e8d8a0')); d.rectangle([5, 2, 10, 4], fill=hexc('b0b0b8')); d.rectangle([5, 11, 10, 13], fill=hexc('b0b0b8')); d.line([7, 5, 8, 10], fill=hexc('c03030'))
        elif idn == 'crowbar':
            d.line([4, 14, 12, 3], fill=hexc('3a3a44'), width=2); d.line([12, 3, 14, 4], fill=hexc('3a3a44'), width=2); d.line([4, 14, 2, 13], fill=hexc('3a3a44'), width=2)
        elif idn == 'ignition_key':
            d.ellipse([2, 2, 8, 8], fill=hexc('d8b04a')); d.ellipse([4, 4, 6, 6], fill=(0, 0, 0, 0)); d.line([7, 7, 13, 13], fill=hexc('c0c4cc'), width=2); d.point((11, 13), hexc('c0c4cc'))
        elif idn == 'spark_plug':
            d.rectangle([6, 1, 9, 6], fill=hexc('f0f0f0')); d.rectangle([5, 7, 10, 10], fill=hexc('909aa4')); d.rectangle([7, 11, 8, 14], fill=hexc('6a6a70'))
        elif idn == 'antenna':
            d.line([8, 14, 8, 2], fill=hexc('b0b4bc'), width=1); d.ellipse([6, 1, 10, 4], fill=hexc('d03030')); d.rectangle([5, 12, 11, 14], fill=hexc('303030'))
        elif idn == 'fuel_hose':
            d.arc([2, 2, 13, 13], 0, 300, fill=hexc('2a2a2a'), width=3); d.rectangle([11, 6, 14, 8], fill=hexc('c0a040'))
        elif idn in ('iron_key', 'brass_key'):
            c = hexc('7a7e86') if idn == 'iron_key' else hexc('d0a040')
            d.ellipse([1, 1, 7, 7], outline=c, width=2); d.line([6, 6, 14, 14], fill=c, width=2); d.line([11, 11, 13, 9], fill=c, width=1); d.line([13, 13, 15, 11], fill=c, width=1)
        elif idn == 'battery_cell':
            for k, x in enumerate((3, 9)):
                d.rectangle([x, 4, x + 4, 13], fill=hexc('2a6a3a')); d.rectangle([x, 4, x + 4, 6], fill=hexc('d0d0d0')); d.rectangle([x + 1, 2, x + 3, 3], fill=hexc('a0a0a0'))
                d.line([x + 2, 8, x + 2, 11], fill=hexc('f0e080'))
        elif idn == 'hammer':
            d.line([4, 14, 10, 6], fill=hexc('8a6033'), width=2); d.rectangle([7, 1, 14, 5], fill=hexc('6a6e76')); d.line([7, 1, 14, 1], fill=hexc('a0a4ac'))
        elif idn == 'flashlight':
            d.rectangle([2, 6, 10, 10], fill=hexc('303036')); d.polygon([(10, 5), (14, 3), (14, 13), (10, 11)], fill=hexc('505058')); d.rectangle([13, 4, 14, 12], fill=hexc('fff2b0'))
        im = big.resize((64, 64), Image.LANCZOS)
        # мягкая тёмная обводка, чтобы иконка читалась на любом фоне
        sh = Image.new('RGBA', im.size, (0, 0, 0, 0)); a = im.split()[3].filter(__import__('PIL.ImageFilter', fromlist=['x']).MaxFilter(3))
        sh.putalpha(a.point(lambda v: v * 0.55)); sh = Image.composite(Image.new('RGBA', im.size, (12, 8, 10, 255)), sh, a.point(lambda v: 0)) if False else sh
        base = Image.new('RGBA', im.size, (0, 0, 0, 0)); base.paste((14, 10, 12, 140), (0, 0), a.point(lambda v: int(v * 0.55))); base.alpha_composite(im)
        base.save(ensure(P('textures', 'item', idn + '.png')))
        json.dump({'parent': 'item/generated', 'textures': {'layer0': 'forest:item/' + idn}}, open(ensure(P('models', 'item', idn + '.json')), 'w'))
    names = {'wheel': 'Колесо', 'jack': 'Домкрат', 'battery': 'Аккумулятор', 'canister': 'Канистра (пустая)', 'canister_full': 'Канистра с бензином', 'paddle': 'Весло',
             'cutters': 'Кусачки', 'fuse': 'Предохранитель', 'crowbar': 'Лом', 'ignition_key': 'Ключ зажигания', 'spark_plug': 'Свеча зажигания', 'antenna': 'Антенна',
             'fuel_hose': 'Топливный шланг', 'flashlight': 'Фонарик', 'iron_key': 'Железный ключ', 'brass_key': 'Латунный ключ', 'hammer': 'Молоток', 'battery_cell': 'Батарейки'}
    lang = {'item.forest.' + k: v for k, v in names.items()}
    lang.update({'entity.forest.carmilla': 'Кармилла', 'entity.forest.eleonora': 'Элеонора', 'entity.forest.tourist': 'Турист',
                 'key.categories.forest': 'Лес', 'key.forest.flash': 'Фонарик', 'key.forest.use': 'Действие (держать)', 'key.forest.drop': 'Бросить предмет',
                 'key.forest.follow': 'Боту: «За мной!»', 'key.forest.wait': 'Боту: «Жди здесь»', 'key.forest.hide': 'Боту: «Прячемся!»', 'key.forest.goto': 'Боту: «Иди туда»', 'key.forest.wheel': 'Колесо фраз (держать)', 'key.forest.yes': 'Принять предложение', 'key.forest.no': 'Отказаться', 'key.forest.search': 'Боту: «Обыщи тут»'})
    for code in ('ru_ru', 'en_us'):
        json.dump(lang, open(ensure(P('lang', code + '.json')), 'w'), ensure_ascii=False, indent=1)

if __name__ == '__main__':
    carmilla(); eleonora()
    tourist(0)
    for i in range(1, 4): tourist(i)   # одна геометрия, разные текстуры — последняя геометрия общая для всех
    anim(); icons()
    print('ассеты готовы')
