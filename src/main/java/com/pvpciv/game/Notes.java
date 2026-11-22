package com.pvpciv.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pvpciv.data.StoryData;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.1: «Заметки» в телефоне — справка по уже открытым системам: что это, где и зачем.
 * Карточка появляется, когда игрок до системы добрался; новых механик тут нет — только память.
 */
public final class Notes {
    private Notes() {}

    private static void card(JsonArray a, boolean open, int color, String title, String text) {
        if (!open) return;
        JsonObject o = new JsonObject();
        o.addProperty("t", title); o.addProperty("x", text); o.addProperty("c", color);
        a.add(o);
    }

    public static void open(ServerPlayer p) {
        StoryData d = Progress.d(p);
        JsonArray a = new JsonArray();
        int gold = 0xFFD9A441, blue = 0xFF7FDBFF, red = 0xFFE05A5A, pink = 0xFFE070C0, violet = 0xFFB070FF, green = 0xFF8AD86A;
        long cap = Progress.penaltyCap(d.level);
        card(a, true, red, "Смерть", "Сгорает 10% искр, но не больше " + cap + " ✦ на твоём уровне, и 15% опыта до уровня. "
                + "На арене босса, в разломе и у Луны — вдвое мягче. Убийство игроком: 25% искр выпадает рядом — их можно подобрать.");
        card(a, true, gold, "Сумка", "Слоты открываются уровнем (строка каждые 10 уровней) и расширителями — по одному с каждого этажа. "
                + "Колесо мыши над сумкой — прокрутка. Сортировка и фильтры — справа.");
        card(a, true, gold, "Квартира", "Кузница (заточка звёздами), разборка (хлам → искры), стол брони, домашний сундук. "
                + "На стенах — витрины трофеев по этажам: ПКМ трофеем. Без трофеев на витрине лифт выше не поедет.");
        card(a, true, blue, "Телефон", "Выкуп из подвала Кармиллы, доставка еды, такси по этажам 1–2. Заметки — здесь.");
        card(a, d.forgeLvl > 1 || d.level >= 3, gold, "Заточка", "Обычная звезда: шанс падает с уровнем заточки. Благословенная — без риска. "
                + "Звезда крови — оружие цело, но платишь свободой (подвал Кармиллы). +10 → +11 — только ритуалом тьмы.");
        card(a, d.questsDone > 0 || d.level >= 6, pink, "Контракты Кармиллы", "Убей N мобов за 20 минут. Чаще всего — мобы твоего уровня; "
                + "плата растёт с их уровнем. Провал — откат. Пять контрактов — титул.");
        card(a, d.liftOk, blue, "Небесный лифт", "Этаж выше открывается, когда пали его боссы и их трофеи стоят на витрине в квартире.");
        card(a, d.level >= 8, violet, "Алтарь инфузии", "Подвал общежития: оружие или особый предмет в центр, звёзды на пьедесталы, ПКМ по алтарю.");
        card(a, d.libQuest > 0, green, "Альманах", "Бестиарий, оружие, броня и качество. Страница зверя заполняется, когда убьёшь его хоть раз.");
        card(a, (d.trophies & 64) != 0, blue, "Ракета «Заря-5»", "Космодром NaZa ↔ ангар станции «Орбита». ПКМ по пульту; кто сел во время отсчёта — летит вместе.");
        card(a, d.gates.getInt("tp5") != 0, blue, "Телепорты «Орбиты»", "Коснись пульта площадки — запомнится. Между запомненными — бесплатно.");
        card(a, d.gates.getInt("luna5_met") > 0 || Classes.matter(p) > 0, pink, "Классы и тёмная материя", "Тёмная материя — баланс (◆ в меню навыков). "
                + "Тратится на дерево классов у Луны. Оружие своего класса сильнее, чужое — слабее.");
        card(a, d.gates.getInt("luna5_met") > 0, pink, "Испытания Луны", "Стадии без конца, каждая — сила следующего этажа. Первая победа на стадии — тёмная материя.");
        card(a, (d.trophies & 128) != 0, violet, "Пояс астероидов", "Шлюз ангара. Низкая гравитация; в центре — алтарь Галактического червя.");
        card(a, (d.trophies & 256) != 0, red, "Демоница Пустоты", "Четыре стадии: цветной пол, лазеры (белые — пригнуться), паркур с рывком и чекпоинтами, волны неуязвимых.");
        card(a, Ritual.open(d), violet, "Ритуал тьмы", "Зал за лавкой Луны. Коснись 16 инжекторов тёмной звездой, положи оружие +10 на ядро, ПКМ. "
                + "Одна звезда сгорает. 50% — +11, иначе оружие рассыпается в осколки темноты.");
        JsonObject j = new JsonObject();
        j.add("cards", a);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(26, j.toString()));
    }
}
