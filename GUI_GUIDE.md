# Гайд по интерфейсу PvP Цивилизации

Всё, что рисуется на экране, проходит через три вещи: **спрайты**, **`Skin`** и **базовые экраны**. Если держаться этих правил, новые окна будут выглядеть так же, как существующие.

## Стиль
- Тёмное фэнтези: обсидиановый фон `#15121F`, золотая филигрань (`GOLD #D9A441`, блик `GOLD_L #FFE08A`, тень `GOLD_D #7A5420`), акцент углей `EMBER #FF8C42`, лёд `ICE #7FDBFF`.
- Текст: заголовки `GOLD_L`, основной `TEXT`, пояснения `DIM`, неактивное `FAINT`; хорошо/внимание/плохо — `GOOD`/`WARN`/`BAD`.
- Из символов в шрифте есть только `★ ☆ ✦ ❤ ☠ » → − ·`. Остальное рисуй иконками.
- Проверяй раскладку на окне 854×480 при масштабе GUI 2 (это 427×240 GUI-пикселей): окно не выше 210, не шире 360.

## Спрайты
- Лежат в `assets/pvpciv/textures/gui/sprites/`, плотность **x2** (1 GUI-пиксель = 2 пикселя текстуры).
- Их рисует скрипт **`tools/gui_sprites.py`** (Python + Pillow, суперсэмплинг x8). Правка или новый спрайт = правка скрипта и `python3 tools/gui_sprites.py`. Руками PNG не редактируем.
- Растягиваемые спрайты — nine-slice: `c.save('имя', (left, top, right, bottom))` в GUI-пикселях, `.mcmeta` пишется сам. Центр и края **тайлятся**, поэтому узор должен повторяться.
- Набор: `panel`, `plate`, `inset`, `header`, `button*`, `tab*`, `slot`, `slot_locked`, `slot_selected`, `bar_frame`, `gem_*`, `icon_*`, `hint_*`, `anvil`, `glow`, `podium`, `crack`, `divider`.

## Skin (`client/ui/Skin.java`)
Единственное место, где вызывается отрисовка спрайтов. Координаты — GUI-пиксели.

| Нужно | Метод |
|---|---|
| Окно | `Skin.panel(g, x, y, w, h)` + `Skin.header(g, font, cx, y - 8, "Заголовок")` |
| Плита HUD | `Skin.plate` |
| Утопленный блок | `Skin.inset` |
| Кнопка (высота 16) | `Skin.button(g, font, x, y, w, "Текст", mx, my, enabled, ember)`; `ember=true` — главное действие |
| Вкладка (14) / строка списка (13) | `Skin.tab`, `Skin.row` |
| Слот | `Skin.slot(g, itemX, itemY)`, `slotLocked`, `hint(g, "weapon", …)`, `rarity(g, itemX, itemY, rgb)` |
| Полоска | `Skin.bar(g, x, y, w, f, colTop, colBot, "red")` (высота 12), `thinBar` |
| Значки | `Skin.icon(g, "spark", x, y)`, `Skin.gem(g, "amber", x, y)`, `Skin.money(...)`, `Skin.stars(...)`, `Skin.divider`, `Skin.glow` |
| Любой спрайт | `Skin.spr(g, "имя", x, y, w, h)` или с оттенком `…, argb)` |

Эффекты — `client/ui/Fx.java`: `fx.burst(x, y, n, rgb, speed)`, `fx.pop("+50", x, y, rgb)`, `fx.flash(rgb, ms)`, `fx.shake(ms)`.

## Новое окно с предметами
1. **Меню** `menu/XxxMenu extends CivMenu`: свои слоты, затем `addBag(itemX, itemY, () -> true)` и `addHotbar(inv, itemX, itemY)`. Координаты слота — это левый верхний угол **предмета** 16×16; рамка рисуется на 1 пиксель шире. Шаг сетки 18. Кнопки обрабатываются в `clickMenuButton`.
2. Регистрация `MenuType` в `ModRegistry` и экрана в `ClientInit` (`RegisterMenuScreensEvent`).
3. **Экран** `client/screen/XxxScreen extends CivScreen<XxxMenu>`: в конструкторе заголовок и размер; в `drawContent` — содержимое (абсолютные координаты: `leftPos + …`). Слоты, редкость, «+N», замки и тултипы рисует база. Подсказки пустых слотов — `hintFor`. Деньги — `drawMoney`. Клики — `mouseClicked` + `in(mx, my, x, y, w, h)` + `click(id)`.
4. Стандартная раскладка станции: левая колонка 152 (сцена `inset` 70 → сведения 52 → кнопка → `stationBlock`), справа сумка 9×7 с `itemX = 169, itemY = 37`, хотбар `itemY = 181`, окно 340×206.

## Новое окно без предметов
`extends PlainScreen("Заголовок", w, h, backToInventory)`, содержимое в `drawContent` (координаты `left + …`, `top + …`). Примеры — `StubScreens`.

## HUD
`client/Hud.java`. Ванильные слои скрыты в `ClientInit` (`RenderGuiLayerEvent.Pre`). Полоса босса, цвета тултипов, экран смерти и сдвиг чата подключены там же.

## Проверка
Каждый экран смотрим в живом клиенте (сервер + клиент, скриншот). Чек-лист: ничего не вылезает за `inset`, тексты не накладываются, кнопки нажимаются там, где нарисованы, окно читается при масштабе 2.
