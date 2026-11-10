# PvP Цивилизация

Мод для Minecraft 1.21.1 (NeoForge 21.1): PvP-цивилизация над Бездной с лутовой прогрессией.

**Скачать последнюю версию:** [Releases → latest](../../releases/latest)

## Установка
1. NeoForge 21.1.x для Minecraft 1.21.1.
2. Положи jar из релиза в `mods`.
3. Новый мир → тип мира «PvP Цивилизация (Бездна)».

## Сборка
```
./gradlew build
```
Jar появится в `build/libs`.

## Выпуск релиза
Поставь тег `vX.Y.Z` и запушь его — GitHub Actions соберёт jar и выложит релиз:
```
git tag v0.3.0 && git push origin v0.3.0
```
