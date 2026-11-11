#!/usr/bin/env python3
"""Быстрый предпросмотр GeckoLib-модели без игры: python3 tools/geo_preview.py <geo.json> <texture.png> <out.png> [yaw...]
Ортографическая проекция, сортировка граней по глубине, текстуры граней через аффинное преобразование.
Анимации не применяются, только запечённые повороты костей и кубов."""
import json, math, sys
import numpy as np
from PIL import Image, ImageDraw


def rot(rx, ry, rz):
    rx, ry, rz = map(math.radians, (rx, ry, rz))
    X = np.array([[1, 0, 0], [0, math.cos(rx), -math.sin(rx)], [0, math.sin(rx), math.cos(rx)]])
    Y = np.array([[math.cos(ry), 0, math.sin(ry)], [0, 1, 0], [-math.sin(ry), 0, math.cos(ry)]])
    Z = np.array([[math.cos(rz), -math.sin(rz), 0], [math.sin(rz), math.cos(rz), 0], [0, 0, 1]])
    return Z @ Y @ X


def quads(geo):
    g = geo['minecraft:geometry'][0]
    bones = {b['name']: b for b in g['bones']}
    def xf(name, p):
        b = bones[name]
        r = b.get('rotation')
        if r:
            pv = np.array(b['pivot'], float)
            p = (rot(-r[0], -r[1], r[2]) @ (p - pv).T).T + pv
        return xf(b['parent'], p) if b.get('parent') else p
    out = []
    for b in g['bones']:
        for c in b.get('cubes', []):
            o, s = np.array(c['origin'], float), np.array(c['size'], float)
            x0, y0, z0 = o; x1, y1, z1 = o + s
            faces = {  # углы: (верх-лево, верх-право, низ-лево) в координатах «как смотрит зритель на грань»
                'north': [(x1, y1, z0), (x0, y1, z0), (x1, y0, z0)],
                'south': [(x0, y1, z1), (x1, y1, z1), (x0, y0, z1)],
                'east': [(x1, y1, z1), (x1, y1, z0), (x1, y0, z1)],
                'west': [(x0, y1, z0), (x0, y1, z1), (x0, y0, z0)],
                'up': [(x1, y1, z1), (x0, y1, z1), (x1, y1, z0)],
                'down': [(x1, y0, z0), (x0, y0, z0), (x1, y0, z1)],
            }
            for fc, pts in faces.items():
                P = np.array(pts, float)
                if c.get('rotation'):
                    pv = np.array(c['pivot'], float); r = c['rotation']
                    P = (rot(-r[0], -r[1], r[2]) @ (P - pv).T).T + pv
                P = xf(b['name'], P)
                uv = c['uv'][fc]
                out.append((P, uv['uv'], uv['uv_size'], fc))
    return out


def render(geo, tex, yaw, size=420, scale=None):
    qs = quads(geo)
    ry = rot(0, yaw, 0)
    pts = np.concatenate([q[0] for q in qs])
    allp = (ry @ pts.T).T
    lo, hi = allp.min(0), allp.max(0)
    span = max(hi[0] - lo[0], hi[1] - lo[1]) * 1.08
    k = scale or size / span
    cx, cy = (lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2
    img = Image.new('RGBA', (size, size), (46, 52, 64, 255))
    items = []
    for P, uv, us, fc in qs:
        Q = (ry @ P.T).T
        # 4-й угол
        n = np.cross(Q[1] - Q[0], Q[2] - Q[0])
        if n[2] <= 0: continue
        depth = Q[:, 2].mean()
        scr = [(size / 2 - (q[0] - cx) * k, size / 2 - (q[1] - cy) * k) for q in Q]
        items.append((depth, scr, uv, us, fc))
    items.sort(key=lambda t: -t[0])
    for depth, scr, uv, us, fc in items:
        (ax, ay), (bx, by), (cx2, cy2) = scr
        w, h = abs(us[0]), abs(us[1])
        if w < 1 or h < 1: continue
        region = tex.crop((uv[0], uv[1], uv[0] + w, uv[1] + h))
        # экран = A + u*(B-A)/w + v*(C-A)/h  → обратное преобразование
        M = np.array([[(bx - ax) / w, (cx2 - ax) / h], [(by - ay) / w, (cy2 - ay) / h]])
        if abs(np.linalg.det(M)) < 1e-6: continue
        Mi = np.linalg.inv(M)
        quad = [scr[0], scr[1], (bx + cx2 - ax, by + cy2 - ay), scr[2]]
        xs, ys = [p[0] for p in quad], [p[1] for p in quad]
        bx0, by0 = int(max(0, min(xs))), int(max(0, min(ys)))
        bx1, by1 = int(min(size, max(xs) + 1)), int(min(size, max(ys) + 1))
        if bx1 <= bx0 or by1 <= by0: continue
        # (u,v) = Mi @ ((x,y) - A)
        a, b, c = Mi[0, 0], Mi[0, 1], -(Mi[0, 0] * (ax - bx0) + Mi[0, 1] * (ay - by0))
        d, e, f = Mi[1, 0], Mi[1, 1], -(Mi[1, 0] * (ax - bx0) + Mi[1, 1] * (ay - by0))
        patch = region.transform((bx1 - bx0, by1 - by0), Image.AFFINE, (a, b, c, d, e, f), resample=Image.NEAREST, fillcolor=(0, 0, 0, 0))
        mask = Image.new('L', patch.size, 0)
        ImageDraw.Draw(mask).polygon([(p[0] - bx0, p[1] - by0) for p in quad], fill=255)
        pa = np.array(patch); ma = np.array(mask)
        pa[..., 3] = np.minimum(pa[..., 3], ma)
        img.alpha_composite(Image.fromarray(pa), (bx0, by0))
    return img


if __name__ == '__main__':
    geo = json.load(open(sys.argv[1])); tex = Image.open(sys.argv[2]).convert('RGBA')
    yaws = [float(a) for a in sys.argv[4:]] or [0, 35, 90, 180]
    tiles = [render(geo, tex, y) for y in yaws]
    out = Image.new('RGBA', (420 * len(tiles), 420))
    for i, t in enumerate(tiles): out.paste(t, (i * 420, 0))
    out.save(sys.argv[3])
