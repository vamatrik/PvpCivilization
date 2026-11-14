#!/usr/bin/env python3
"""0.10: выстрел автомата и «прицеливание» (короткий тон-телеграф). Пишет fx/shot.ogg, fx/aim.ogg и добавляет в sounds.json."""
import json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
import numpy as np
SR = 44100
rng = np.random.default_rng(11)
ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources', 'assets', 'pvpciv')
OUT = os.path.join(ROOT, 'sounds', 'fx')
import subprocess, wave
def save(name, x, vol=0.8):
    x = x / (np.max(np.abs(x)) + 1e-9) * vol
    wav = '/tmp/' + name + '.wav'
    with wave.open(wav, 'w') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((x * 32767).astype(np.int16).tobytes())
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '5', os.path.join(OUT, name + '.ogg')], check=True)
def lp(x, k):
    y = np.zeros_like(x); a = 0.0
    for i, v in enumerate(x): a += k * (v - a); y[i] = a
    return y
x = np.arange(int(SR * 0.16)) / SR
crack = rng.standard_normal(len(x)) * np.exp(-x / 0.012)
body = lp(rng.standard_normal(len(x)), 0.18) * np.exp(-x / 0.05) * 1.6
thump = np.sin(2 * np.pi * (95 * x - 120 * x * x)) * np.exp(-x / 0.03) * 1.2
save('shot', crack * 0.7 + body + thump, 0.85)
x = np.arange(int(SR * 0.5)) / SR
f = 900 + 500 * x / 0.5
tone = np.sin(2 * np.pi * np.cumsum(f) / SR) * (0.5 + 0.5 * (np.sin(2 * np.pi * 18 * x) > 0))
save('aim', tone * np.clip(x / 0.02, 0, 1) * np.clip((0.5 - x) / 0.05, 0, 1), 0.35)
p = os.path.join(ROOT, 'sounds.json'); d = json.load(open(p))
d['fx.shot'] = {'category': 'hostile', 'sounds': ['pvpciv:fx/shot']}
d['fx.aim'] = {'category': 'hostile', 'sounds': ['pvpciv:fx/aim']}
json.dump(d, open(p, 'w'), indent=2, ensure_ascii=False)
print('ok')
