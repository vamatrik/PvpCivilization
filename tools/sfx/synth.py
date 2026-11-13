#!/usr/bin/env python3
"""Синтезированные звуки (0.9.0): неоновые эффекты, крит, загадка Юки, врата. Пишет .ogg в assets/pvpciv/sounds/fx и обновляет sounds.json."""
import json, os, subprocess, wave
import numpy as np
SR = 44100
ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'assets', 'pvpciv')
OUT = os.path.join(ROOT, 'sounds', 'fx'); os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(7)
def t(sec): return np.arange(int(SR * sec)) / SR
def env(n, a=0.005, r=0.3, sec=None):
    x = np.arange(n) / SR; L = n / SR
    return np.clip(x / a, 0, 1) * np.exp(-x / max(r, 1e-3)) if sec is None else np.clip(x / a, 0, 1) * np.clip((L - x) / (L * r), 0, 1)
def noise(n): return rng.standard_normal(n)
def lp(x, k=0.1):
    y = np.zeros_like(x); acc = 0.0
    for i, v in enumerate(x): acc += k * (v - acc); y[i] = acc
    return y
def save(name, x, vol=0.8):
    x = x / (np.max(np.abs(x)) + 1e-9) * vol
    wav = os.path.join('/tmp', name + '.wav')
    with wave.open(wav, 'w') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((x * 32767).astype(np.int16).tobytes())
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5', os.path.join(OUT, name + '.ogg')], check=True)
# телеграф зоны: нарастающий неоновый гул с тремоло
x = t(0.9); f = 180 + 260 * x / 0.9
s = np.sin(2 * np.pi * np.cumsum(f) / SR) * (0.6 + 0.4 * np.sin(2 * np.pi * 14 * x)) + 0.3 * np.sin(2 * np.pi * np.cumsum(f * 2.01) / SR)
save('zone', s * np.clip(x / 0.6, 0, 1) * np.clip((0.9 - x) / 0.08, 0, 1), 0.55)
# удар: низкий бум + шипящий хвост
x = t(0.8); b = np.sin(2 * np.pi * (70 * x - 25 * x * x)) * np.exp(-x / 0.18)
save('impact', b + 0.35 * lp(noise(len(x)), 0.25) * np.exp(-x / 0.12), 0.9)
# ударная волна: свист вниз
x = t(1.0); f = 900 * np.exp(-x * 2.5) + 120
save('wave', (np.sin(2 * np.pi * np.cumsum(f) / SR) * 0.6 + 0.4 * lp(noise(len(x)), 0.05)) * np.exp(-x / 0.4) * np.clip(x / 0.02, 0, 1), 0.6)
# крит: два металлических звона
x = t(0.5)
def bell(f0): return sum(np.sin(2 * np.pi * f0 * m * x) * np.exp(-x / (0.25 / m)) / m for m in (1, 2.76, 5.4))
save('crit', bell(1320) + 0.6 * np.concatenate([np.zeros(int(SR * 0.05)), bell(1760)[:-int(SR * 0.05)]]), 0.6)
# загадка: верно (арпеджио) / неверно (низкий «бзз»)
x = t(0.12); notes = [523, 659, 784, 1047]
save('word_ok', np.concatenate([np.sin(2 * np.pi * n * x) * env(len(x), 0.004, 0.06) for n in notes]), 0.5)
x = t(0.35); save('word_bad', np.sign(np.sin(2 * np.pi * 110 * x)) * 0.4 * np.exp(-x / 0.2) + 0.3 * np.sin(2 * np.pi * 104 * x) * np.exp(-x / 0.2), 0.45)
# врата: кристаллический шорох и аккорд
x = t(1.6); ch = sum(np.sin(2 * np.pi * f * x + 0.3 * np.sin(2 * np.pi * 5 * x)) for f in (392, 494, 587, 784)) * np.exp(-x / 0.7) * np.clip(x / 0.15, 0, 1)
save('gate', ch + 0.25 * lp(noise(len(x)), 0.4) * np.exp(-x / 0.3), 0.5)
# всплеск блюдца каппы
x = t(0.6); save('splash', lp(noise(len(x)), 0.5) * np.exp(-x / 0.12) + 0.5 * np.sin(2 * np.pi * (600 - 400 * x) * x) * np.exp(-x / 0.08), 0.7)
sj = os.path.join(ROOT, 'sounds.json')
d = json.load(open(sj, encoding='utf-8'))
for n in ('zone', 'impact', 'wave', 'crit', 'word_ok', 'word_bad', 'gate', 'splash'):
    d['fx.' + n] = {"category": "hostile" if n in ('zone', 'impact', 'wave', 'splash') else "player", "sounds": ["pvpciv:fx/" + n]}
json.dump(d, open(sj, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print('sfx ok')
