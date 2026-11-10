#!/usr/bin/env python3
"""Собирает шрифт интерфейса assets/pvpciv/font/ui.ttf из Alegreya Sans Medium (лицензия OFL, см. font/ofl.txt).
Нужны fonttools и исходный файл: https://github.com/google/fonts/tree/main/ofl/alegreyasans
Запуск: python3 tools/make_font.py путь/к/AlegreyaSans-Medium.ttf
Что делает: заменяет «старинные» цифры на ровные (lnum) и переименовывает семейство в «PvpCiv UI».
Размер и сдвиг задаются в assets/minecraft/font/default.json (size 11 = высота заглавной 7 px, как у ванильного)."""
import sys, os
from fontTools.ttLib import TTFont
f = TTFont(sys.argv[1]); m = {}
for fr in f['GSUB'].table.FeatureList.FeatureRecord:
    if fr.FeatureTag == 'lnum':
        for li in fr.Feature.LookupListIndex:
            for st in f['GSUB'].table.LookupList.Lookup[li].SubTable:
                if hasattr(st, 'mapping'): m.update(st.mapping)
for t in f['cmap'].tables:
    for cp, name in list(t.cmap.items()):
        if 0x30 <= cp <= 0x39 and name in m: t.cmap[cp] = m[name]
for rec in f['name'].names:
    if rec.nameID in (1, 4, 16): rec.string = 'PvpCiv UI'
    if rec.nameID == 6: rec.string = 'PvpCivUI-Medium'
f.save(os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/pvpciv/font/ui.ttf'))
print('ok')
