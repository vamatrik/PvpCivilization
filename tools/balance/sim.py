#!/usr/bin/env python3
"""Симуляция баланса этажей 1–2 по формулам мода.
Модель: игрок типичного уровня и экипировки зоны против мобов зоны.
Считаем: удары до убийства, время убийства (TTK), урон по игроку в секунду от одного моба и от толпы,
сколько секунд игрок живёт в толпе, сколько мобов успеет убить подряд с полного HP.
Запуск: python3 tools/balance/sim.py [old|new]"""
import math, sys

MODE = sys.argv[1] if len(sys.argv) > 1 else 'old'
NEW = MODE == 'new'

RAR = {'common': 1.0, 'uncommon': 1.15, 'rare': 1.35, 'epic': 1.6}
CLS = {'sword': (1.0, 1.6), 'saber': (0.85, 2.0), 'axe': (1.3, 1.0)}

def mob_hp(l):  return 20 * 1.065 ** (l - 1)
def mob_dmg(l): return 3 * (MD if NEW else 1.05) ** (l - 1)
MD = 1.07
HPA, HPB = 3, 0.2
def tier_base(t):
    return 4 * (1.35 if not NEW else 1.45) ** (t - 1)

def weapon(t, cls, rar, stars):
    star = 2.6 if stars >= 11 else 1 + 0.1 * stars
    return tier_base(t) * CLS[cls][0] * RAR[rar] * star

def armor_mult(stars): return 2.3 if stars >= 11 else 1 + 0.08 * stars
def armor_pts(t, rar, stars):  return 8 * 1.25 ** (t - 1.5) * RAR[rar] * armor_mult(stars)
def armor_hp(t, rar, stars):   return 5 * 1.3 ** (t - 1.5) * RAR[rar] * armor_mult(stars)

def vanilla_armor(dmg, armor, tough=0):
    f = max(armor * 0.2, armor - dmg / (2 + tough / 4))
    f = min(20, f)
    return dmg * (1 - f / 25)

def resist(dl):  # урон игрока по мобу выше уровнем
    return min(1, max(0, 0.25 * (dl - 1)))

def player_taken_mult(dl):  # dl = моб − игрок
    if dl > 0: return 1 + 0.15 * dl
    if not NEW: return 1
    below = -dl
    return {0: 1, 1: 0.8, 2: 0.7, 3: 0.6, 4: 0.5}.get(below, 0.4)

def crowd_mult(k):
    if not NEW or k <= 1: return 1
    return 1 / (1 + 0.10 * (k - 1))

# kind: (интервал атаки, с; множитель урона за атаку; доля попаданий)
KINDS = {
    'zombie': (1.0, 1.0, 0.8), 'spider': (1.0, 1.0, 0.8),
    'archer_old': (2.0, None, 0.6),   # ванильная стрела ≈4 независимо от уровня
    'archer':  (2.0, 0.9, 0.6),
    'beast':   (0.75, 0.8, 0.8),      # крысы/собаки — быстрые укусы
    'gunner_old': (2.0, 3 * 1.19, 0.55),  # 3 пули × 3.4 скорость × 0.35 урона
    'gunner':  (2.0, 3 * 0.45, 0.6),
}

ZONES = [
    # имя, уровень игрока, (тир, класс, редкость, заточка), броня (тир, редкость, заточка) или None, уровни мобов, состав, мобов в толпе
    ('Пустошь (старт)', 2, (1, 'sword', 'common', 1), None, (1, 2), ['zombie', 'spider'], 3),
    ('Пустошь (юг)',    4, (1, 'sword', 'uncommon', 3), None, (3, 4), ['zombie', 'archer', 'zombie'], 4),
    ('Кряж',            6, (1.5, 'sword', 'uncommon', 4), (1.5, 'common', 2), (5, 7), ['zombie', 'archer', 'spider'], 4),
    ('Город, этаж 2 (как пришёл)', 7, (1.5, 'sword', 'uncommon', 4), (1.5, 'common', 2), (6, 8), ['zombie', 'zombie', 'beast'], 5),
    ('Город, этаж 2',   8, (2, 'sword', 'uncommon', 3), (1.5, 'uncommon', 4), (6, 8), ['zombie', 'zombie', 'beast'], 5),
    ('Канализация',     9, (2, 'sword', 'uncommon', 3), (1.5, 'uncommon', 4), (9, 10) if not NEW else (8, 9), ['beast', 'beast', 'zombie', 'gunner'], 5),
    ('Канализация (люки)', 10, (2.5, 'sword', 'uncommon', 3), (2.5, 'common', 2), (9, 10), ['beast', 'zombie', 'gunner'], 4),
]

def zone_report(name, pl, w, arm, lv, kinds, crowd):
    wdmg = weapon(*w) * (1 + (0.015 if NEW else 0.006) * (pl - 1))
    aps = CLS[w[1]][1] * 0.85          # реальный темп с промахами
    lvl_hp = sum(HPA + HPB * (i - 2) for i in range(2, pl + 1)) if NEW else (pl - 1)
    hp = 20 + lvl_hp + (armor_hp(*arm) * (1.25 if NEW else 1) if arm else 0)
    ap = armor_pts(*arm) * (1.25 if NEW else 1) if arm else 0
    rows = []
    for ml in range(lv[0], lv[1] + 1):
        dl = ml - pl
        hits = math.ceil(mob_hp(ml) / (wdmg * (1 - resist(dl)))) if resist(dl) < 1 else 999
        ttk = hits / aps
        dps = 0
        for k in kinds:
            kk = k + '_old' if (not NEW and k in ('archer', 'gunner')) else k
            iv, mul, acc = KINDS[kk]
            raw = 4 if mul is None else mob_dmg(ml) * mul
            if kk.startswith('gunner'):
                per = (raw / 3) if not NEW else raw / 3
                per = math.ceil(per) if not NEW else per
                raw = per * 3
            hit = vanilla_armor(raw * player_taken_mult(dl), ap)
            dps += hit * acc / iv
        dps /= len(kinds)
        solo_fights = hp / (dps * ttk) if dps else 99
        crowd_dps = dps * crowd * crowd_mult(crowd)
        surv = hp / crowd_dps
        # убить всю толпу: мобы падают по одному, урон убывает
        t = 0; taken = 0
        for i in range(crowd, 0, -1):
            taken += dps * i * crowd_mult(i) * ttk; t += ttk
        rows.append((ml, hits, ttk, dps, solo_fights, surv, taken / hp))
    print(f'\n{name}: игрок ур.{pl}, удар {wdmg:.1f}, HP {hp:.0f}, броня {ap:.1f}')
    print('  ур.моба  ударов  TTK,с  урон/с(1)  мобов подряд  жить в толпе {}, с  толпа съест HP'.format(crowd))
    for ml, hits, ttk, dps, sf, surv, frac in rows:
        print(f'  {ml:>6}  {hits:>6}  {ttk:>5.1f}  {dps:>8.2f}  {sf:>12.1f}  {surv:>20.1f}  {frac*100:>10.0f}%')

print('РЕЖИМ:', 'новый баланс' if NEW else 'как сейчас (0.8.4)')
for z in ZONES: zone_report(*z)
