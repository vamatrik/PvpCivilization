package com.pvpciv.game;

import com.pvpciv.entity.GuardEntity;
import com.pvpciv.entity.LunaEntity;
import com.pvpciv.entity.TraderEntity;
import com.pvpciv.game.DialogueManager.Dialogue;
import com.pvpciv.item.SimpleCivItem;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

import static com.pvpciv.game.DialogueManager.opt;

/** Тексты NPC. */
public final class Dialogues {
    private static final String TIM = "Тимофей Глыба";
    private static final int TC = GuardEntity.COLOR;
    private static final String LUNA = "Луна";
    private static final int LC = LunaEntity.COLOR;

    public static Dialogue guardIntro(GuardEntity g) {
        return new Dialogue("a")
                .node("a", TIM, TC, "О, проснулся. Доброе утро, новенький. Я Тимофей Глыба — охрана этого прекрасного... общежития.",
                        opt("Где я?", "where"), opt("Можно я обратно спать?", "sleep"))
                .node("where", TIM, TC, "Нижний Шпиль. Цивилизация над Бездной. Тут все дерутся, растут в уровнях и иногда падают вниз. Падать не советую — дорого.",
                        opt("Дорого?", "death"))
                .node("sleep", TIM, TC, "Можно. Но Комендантша Агафья страшнее Бездны. Слушай правила, это быстро.",
                        opt("...Слушаю", "death"))
                .node("death", TIM, TC, "Умрёшь — проснёшься у себя в кровати. Но часть Искр сгорит, и 15% опыта до уровня тоже. От игрока помрёшь — он подберёт четверть твоих денег.",
                        opt("Жёстко", "loot"), opt("А Искры это...", "loot"))
                .node("loot", TIM, TC, "Искры — деньги. Падают с мобов, валяются в тайниках. Хлам продавай Сороке, она у фонтана в атриуме. Стучит как сорока, торгуется как дракон.",
                        opt("Где мобы?", "floor"))
                .node("floor", TIM, TC, "Южный ВЫХОД, пристань, Ледяной Тракт — и ты в Пепельной пустоши, мобы 1–4 уровня. На севере за Ледяной Трассой — Инеевый Кряж, там 5–7. Враг сильнее тебя на 5 уровней — даже не царапнешь.",
                        opt("А лодка?", "boat"), opt("А что выбивать?", "gear"))
                .node("boat", TIM, TC, "Лодка у тебя в инвентаре. ПКМ по льду — поставил, ПКМ рядом — убрал. Потерять её нельзя, даже если очень постараешься. Гриша старался.",
                        opt("А что выбивать?", "gear"))
                .node("gear", TIM, TC, "Оружие разной редкости, звёзды заточки, хлам. В квартире у тебя кузница: ПКМ по ней, оружие — на наковальню, звезду — в слот, «Заточить». Обычная звезда при неудаче ломает меч в обломки.",
                        opt("А если не повезёт?", "bless"), opt("Понял, погнал!", "bye"))
                .node("bless", TIM, TC, "Благословенная звезда при неудаче снимает только 1 заточку — берегите их. Звезду крови лучше не трогай: оружие уцелеет, а тебя самого заберут в подвал. Хлам — на разборочный стол у окна. И на R у тебя Рывок.",
                        opt("Понял, погнал!", "bye"))
                .node("bye", TIM, TC, "Иди. И это... на соседку внимания не обращай. Если встретишь. Она... своеобразная.",
                        opt("[Уйти]", null))
                .onEnd(p -> {
                    Story.setStage(p, Story.GUARD_DONE);
                    Story.title(p, "ЭТАЖ 1 ЖДЁТ", "Южный выход → Ледяной Тракт", 0xFFD166);
                    p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.2f);
                    g.leaveSoon();
                });
    }

    public static Dialogue lunaRepeat(ServerPlayer p) {
        String[] lines = {
                "Опять ты? Я занята. Смотрю, как трава растёт — это интереснее.",
                "О, Пепел. Ну и какой у тебя уровень? ...Хи-хи. Мило.",
                "Громила опять храпит на юге. Сходи, разбуди. Я посмотрю, как он тебя размажет.",
                "Заточил что-нибудь до +5? Нет? Вот и не мешай.",
                "У меня меч +10. Шучу. Или нет. Угадай.",
                "Скоро я разрешу тебе со мной подраться. Готовь кровать — проснёшься в ней.",
        };
        String l = lines[p.getRandom().nextInt(lines.length)];
        return new Dialogue("a").node("a", LUNA, LC, l, opt("[Уйти]", null));
    }

    // --------------------------------------------------------------- комната Луны
    private static final String[] DIARY = {
            "День 1. Новый сосед. Выглядит как пепел, пахнет как пепел. Буду звать его Пеплом. Ему подходит.",
            "День 4. Пепел опять умер у Громилы. Я видела из окна. Не смеялась. Ну, немножко.",
            "День 9. Заточила меч до +7. Никому не скажу. Пусть думают, что +10. (Пеплу особенно.)",
            "День 12. Сорока продала мне «редкую» шестерёнку за 200 Искр. Она ржавая. Я её храню. Не спрашивай.",
            "День 15. Снилась Бездна. Там внизу кто-то смотрел наверх. Он знал моё имя. …Надо меньше пить какао на ночь.",
            "День 20. Если Пепел когда-нибудь это прочтёт — ты ИДИОТ, и это не комплимент. ЗАКРОЙ ДНЕВНИК.",
            "День 23. Подвал. Там кто-то поёт по ночам. Красивым голосом. Комендант сказала «трубы». Трубы не поют на латыни.",
            "День 31. Обогнала всех на Ледяной Трассе. Пепел даже не стартовал. Лузер. (Надо будет научить его. Просто чтобы было с кем соревноваться.)",
    };

    public static Dialogue lunaDiary(ServerPlayer p) {
        Dialogue d = new Dialogue("p0");
        for (int i = 0; i < DIARY.length; i++) {
            String next = i + 1 < DIARY.length ? "p" + (i + 1) : null;
            if (next != null) d.node("p" + i, "Дневник Луны", LC, DIARY[i], opt("Перевернуть страницу", next), opt("[Закрыть]", null));
            else d.node("p" + i, "Дневник Луны", LC, DIARY[i], opt("[Закрыть]", null));
        }
        return d;
    }

    public static void lunaBox(ServerPlayer p) {
        com.pvpciv.data.StoryData d = Progress.d(p);
        if (d.lunaBox) {
            Story.say(p, "Шкатулка", LC, "Плюшевый «ты» лежит на месте и смотрит с укором. Рядом записка: «Я ЗНАЮ, ЧТО ЭТО БЫЛ ТЫ».");
            return;
        }
        d.lunaBox = true;
        p.setData(ModRegistry.STORY.get(), d);
        com.pvpciv.inv.Bag.give(p, new ItemStack(ModRegistry.STAR_W1.get(), 2));
        Story.say(p, "Шкатулка", LC, "Внутри — плюшевая игрушка, подозрительно похожая на тебя. Ты её рассматриваешь… и кладёшь обратно — Луна ведь сразу заметит. Под ней — две звезды заточки. Их она, может, и не хватится.");
        p.level().playSound(null, p.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.8f, 1.3f);
    }

    // --------------------------------------------------------------- Сорока
    private static final Set<Item> JUNK = Set.of();

    private static boolean isJunk(Item it) {
        return it == ModRegistry.RUSTY_GEAR.get() || it == ModRegistry.CHARRED_COIN.get() || it == ModRegistry.ASH_RELIC.get() || it == ModRegistry.WEAPON_SHARDS.get() || it == ModRegistry.FROZEN_LOCKET.get();
    }

    private static long valueOf(ServerPlayer p, boolean withDust) {
        long v = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.getItem() instanceof SimpleCivItem s && (isJunk(st.getItem()) || (withDust && st.is(ModRegistry.ASH_DUST.get()))))
                v += (long) s.sellPrice * st.getCount();
        }
        return v;
    }

    private static void sell(ServerPlayer p, boolean withDust) {
        long v = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.getItem() instanceof SimpleCivItem s && (isJunk(st.getItem()) || (withDust && st.is(ModRegistry.ASH_DUST.get())))) {
                v += (long) s.sellPrice * st.getCount();
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (v > 0) {
            Progress.addMoney(p, v, false);
            p.sendSystemMessage(Component.literal("Продано на " + v + " ✦").withStyle(ChatFormatting.GOLD));
            p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1f, 1.4f);
        }
    }

    public static Dialogue trader(ServerPlayer p) {
        long junk = valueOf(p, false), all = valueOf(p, true);
        String S = "Сорока";
        int C = TraderEntity.COLOR;
        Dialogue d = new Dialogue("a");
        if (all == 0) {
            d.node("a", S, C, "Пустые карманы? Ну ты и голодранец! Приходи, когда натаскаешь хлама с этажа. Шестерёнки, монеты, обломки — всё беру!",
                    opt("Сколько у меня Искр?", "money"), opt("[Уйти]", null));
        } else {
            d.node("a", S, C, "О-о-о, блестяшки! Покажи-ка... Так, за хлам дам " + junk + " ✦. А если ещё и пепельную пыль отдашь — " + all + " ✦. Пыль, кстати, для брони пригодится. Решай!",
                    opt("Продать хлам (+" + junk + " ✦)", "sold"), opt("Продать хлам и пыль (+" + all + " ✦)", "sold2"), opt("Сколько у меня Искр?", "money"), opt("[Уйти]", null));
        }
        d.node("sold", S, C, "Приятно иметь с тобой дело! Заходи ещё, блестящий ты мой.", pp -> sell(pp, false), opt("[Уйти]", null));
        d.node("sold2", S, C, "Всё забираю! Ты лучший клиент этой недели. Ну, после Гриши. Гриша продал мне свою дверь.", pp -> sell(pp, true), opt("[Уйти]", null));
        d.node("money", S, C, "У тебя " + Progress.d(p).money + " ✦. Носи аккуратно: помрёшь — часть сгорит. Сейф для Искр Комендантша обещала поставить в квартиры. Когда-нибудь.",
                opt("[Уйти]", null));
        return d;
    }
}
