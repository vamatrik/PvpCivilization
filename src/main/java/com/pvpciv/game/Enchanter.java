package com.pvpciv.game;

import com.pvpciv.combat.Mobs;
import com.pvpciv.data.StoryData;
import com.pvpciv.inv.Bag;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModF4;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

import static com.pvpciv.game.DialogueManager.opt;

/**
 * 0.11 — Чарователь Аурелий, мастер молекулярного стола NaZa. Квесты открывают новые чары:
 *  1 «Пропуск»: принести 10 лазуратрона → пропуск, чары: Нанозаточка, Криоудар, Бронеплёнка, Сервопривод (+ Базовый модуль).
 *  2 «Образцы»: 20 мобов Оранжереи и Криолаборатории → Вампиризм, Регенератор (+ Базовый модуль).
 *  3 «Отказ систем»: 12 турелей/роботов/охранников → Разряд, Казнь, Отражатель (+ Резонатор).
 *  4 «Ковчег»: одолеть доктора Ноэля → Пробой, Фазовый сдвиг (+ Улучшенный модуль).
 */
public final class Enchanter {
    private Enchanter() {}
    public static final int C = 0x40D0FF;
    public static final String NAME = "Чарователь Аурелий";
    private static final int[] NEED = {0, 0, 20, 12, 1};

    public static void onKill(ServerPlayer p, Mob mob) {
        StoryData d = Progress.d(p);
        int q = Enchanting.questStage(d);
        Mobs.Kind k = Mobs.Kind.of(mob.getType());
        if (k == null) return;
        boolean hit = q == 1 && (k == Mobs.Kind.BOTANIST || k == Mobs.Kind.SPORELING || k == Mobs.Kind.THAWED)
                || q == 2 && (k == Mobs.Kind.TURRET || k == Mobs.Kind.ROBOT || k == Mobs.Kind.SENTRY);
        if (!hit) return;
        int n = d.gates.getInt("ench_k") + 1;
        d.gates.putInt("ench_k", n);
        p.setData(ModRegistry.STORY.get(), d);
        int need = NEED[q + 1];
        if (n <= need) p.displayClientMessage(Component.literal("⚛ Квест Аурелия: " + n + "/" + need + (n == need ? " — возвращайся!" : "")).withStyle(x -> x.withColor(C)), true);
    }

    public static void onBoss(ServerPlayer p) {
        StoryData d = Progress.d(p);
        if (Enchanting.questStage(d) == 3) { d.gates.putInt("ench_k", 1); p.setData(ModRegistry.STORY.get(), d); }
    }

    public static void talk(ServerPlayer p, com.pvpciv.entity.AnimeNpc npc) {
        StoryData d = Progress.d(p);
        int q = Enchanting.questStage(d), k = d.gates.getInt("ench_k");
        DialogueManager.Dialogue dl = new DialogueManager.Dialogue("a");
        if (q == 0) {
            int laz = Bag.count(p, s -> s.is(ModF4.LAZURATRON.get()));
            dl.node("a", NAME, C, "Посетитель? В NaZa? Ха. Я Аурелий, ведущий… единственный оставшийся специалист по молекулярной перешивке. Видишь стол? Он вплетает свойства прямо в кристаллическую решётку клинка.",
                            opt("Зачаруешь мне меч?", "b"), opt("[Уйти]", null))
              .node("b", NAME, C, "Зачарую? Это не магия, это наука! …Ладно, почти магия. Стол работает на лазуратроне — синие кристаллы, их полно в куполах. Принеси 10 штук на калибровку, и я выпишу тебе пропуск сотрудника.",
                            laz >= 10 ? new DialogueManager.Opt[]{opt("Вот, 10 лазуратрона", "c"), opt("Позже", null)} : new DialogueManager.Opt[]{opt("Пойду поищу (" + laz + "/10)", null)})
              .node("c", NAME, C, "Чистые! Отлично. Держи пропуск и Базовый модуль — поставь его у стола, ПКМ. Чем больше модулей, тем больше вариантов на выбор. Работаем так: берёшь вещь в руку, ПКМ по столу.", pp -> advance(pp, 1), opt("Понял", null));
        } else if (q == 1 && k >= NEED[2] || q == 2 && k >= NEED[3] || q == 3 && k >= NEED[4]) {
            String[] text = {"", "", "Образцы! Ну, то, что от них осталось. Из их спор я синтезировал две новые формулы: Вампиризм и Регенератор. И ещё один модуль — бери.",
                    "Охранные системы молчат? Прекрасно. Значит, в Реакторе можно снять схемы: Разряд, Казнь, Отражатель. И резонатор — для самых смелых синтезов.",
                    "Ноэль… Он был моим научным руководителем. Лиана была его гордостью. Спасибо, что закончил это. Последние формулы — Пробой и Фазовый сдвиг. И Улучшенный модуль: теперь стол достаёт до III ранга."};
            dl.node("a", NAME, C, text[q + 1], pp -> advance(pp, q + 1), opt("Спасибо", null));
        } else if (q == 1) {
            dl.node("a", NAME, C, "Нужны образцы: " + k + "/" + NEED[2] + ". Ботаники, споровые ползуны, размороженные — Оранжерея на севере, Криолаборатория на востоке. Не нюхай споры.", opt("Иду", null));
        } else if (q == 2) {
            dl.node("a", NAME, C, "Реактор на юге охраняют турели, роботы и охранники. Отключи " + NEED[3] + " — сейчас " + k + ". Турель телеграфирует лазером: увидел красный луч — уходи вбок.", opt("Иду", null));
        } else if (q == 3) {
            dl.node("a", NAME, C, "Последнее. В западном куполе, «Ковчеге», сидит доктор Ноэль со своей… Лианой. Пока растение живо — Ноэль неуязвим. Сначала срежь её. Если она тебя проглотит — жми пробел, быстро!", opt("Понял", null));
        } else {
            String[] hi = {"Режим x10 дорогой, но три попытки ранга и два лишних варианта. Наука любит щедрых.",
                    "Якорь сохраняет старую формулу, если новые не понравились. Фокус — лишний вариант. Резонатор — ранг выше. И все работают вместе!",
                    "Продвинутые модули найдутся выше. Тогда стол достанет до IV ранга. Наверное. Я не проверял."};
            dl.node("a", NAME, C, hi[p.getRandom().nextInt(hi.length)], opt("[Уйти]", null));
        }
        DialogueManager.open(p, npc, dl);
    }

    private static void advance(ServerPlayer p, int to) {
        StoryData d = Progress.d(p);
        if (to == 1) {
            if (Bag.take(p, s -> s.is(ModF4.LAZURATRON.get()), 10) > 0) return;
            Bag.give(p, new ItemStack(ModF4.NAZA_BADGE.get()));
            Bag.give(p, new ItemStack(ModF4.MODULE_BASIC.get()));
        } else if (to == 2) Bag.give(p, new ItemStack(ModF4.MODULE_BASIC.get()));
        else if (to == 3) Bag.give(p, new ItemStack(ModF4.CAT_RESONATOR.get()));
        else if (to == 4) Bag.give(p, new ItemStack(ModF4.MODULE_IMPROVED.get()));
        d = Progress.d(p);
        d.gates.putInt("ench_q", to);
        d.gates.putInt("ench_k", 0);
        p.setData(ModRegistry.STORY.get(), d);
        Progress.addXp(p, Progress.xpFor(d.level, 16 + to * 2, 20));
        String[] q = {"", "Пропуск получен: стол открыт", "Новые чары: Вампиризм, Регенератор", "Новые чары: Разряд, Казнь, Отражатель", "Новые чары: Пробой, Фазовый сдвиг"};
        Story.title(p, "Молекулярный стол", q[to], C);
        if (to < 4) p.sendSystemMessage(Component.literal(new String[]{"", "Квест «Образцы»: 20 мобов Оранжереи и Криолаборатории.", "Квест «Отказ систем»: 12 турелей, роботов и охранников Реактора.",
                "Квест «Ковчег»: одолей доктора Ноэля."}[to]).withStyle(x -> x.withColor(C)));
    }
}
