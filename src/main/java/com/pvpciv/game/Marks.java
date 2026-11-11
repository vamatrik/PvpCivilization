package com.pvpciv.game;

import com.pvpciv.data.StoryData;
import com.pvpciv.entity.EleonoraEntity;
import com.pvpciv.entity.boss.CivBoss;
import com.pvpciv.floor.BossManager;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

import static com.pvpciv.game.DialogueManager.opt;

/**
 * Метки Элеоноры — испытания на боссов. Раз в день на каждого побеждённого босса — одна метка с условием.
 * Награда — жетоны охотницы (их не трогает инфляция Искр), тратятся в её лавке. Баланс виден только в разговоре с ней.
 * Во время боя по метке Элеонора может позвонить «просто поболтать» — не ответишь или ответишь грубо, она позвонит Кармилле.
 */
public final class Marks {
    public enum Type {
        FAST("Быстрая охота", 2), CLEAN("Чистая работа", 3), NOFOOD("Пост", 1), STEADY("Холодная голова", 2);
        public final String title; public final int bonus;
        Type(String t, int b) { title = t; bonus = b; }
    }

    private static final Map<String, Integer> BASE = Map.of("brute", 4, "keeper", 6, "don", 7, "rat_king", 9);
    private static final Map<String, Integer> FAST_S = Map.of("brute", 90, "keeper", 120, "don", 120, "rat_king", 150);

    public static long day() { return java.time.LocalDate.now(java.time.ZoneId.of("Europe/Moscow")).toEpochDay(); }

    /** Метка дня для босса (у всех игроков одна и та же). */
    public static Type today(String boss) {
        int idx = 0;
        for (int i = 0; i < BossManager.DEFS.size(); i++) if (BossManager.DEFS.get(i).id().equals(boss)) idx = i;
        return Type.values()[(int) Math.floorMod(day() + idx, (long) Type.values().length)];   // в один день у всех боссов разные условия
    }

    public static int reward(String boss, Type t) { return BASE.getOrDefault(boss, 4) + t.bonus; }

    public static String rule(String boss, Type t) {
        return switch (t) {
            case FAST -> "убей за " + FAST_S.getOrDefault(boss, 120) + " секунд с первого удара";
            case CLEAN -> "ни разу не попади под красную зону или ударную волну";
            case NOFOOD -> "не ешь ничего, пока идёт бой";
            case STEADY -> "не опускайся ниже 40% здоровья";
        };
    }

    // ------------------------------------------------------------------ бой
    private static final class Fight {
        final String boss; final long start; String failed; boolean called; long callAt = -1, ringUntil = -1;
        Fight(String b, long s) { boss = b; start = s; }
    }
    private static final Map<UUID, Fight> FIGHTS = new HashMap<>();

    private static Type active(StoryData d, String boss) {
        if (d.eleMark.isEmpty() || !d.eleMark.startsWith(boss + ":")) return null;
        try { return Type.valueOf(d.eleMark.substring(boss.length() + 1)); } catch (Exception e) { return null; }
    }

    /** Игрок ударил босса. */
    public static void onHitBoss(ServerPlayer p, CivBoss b) {
        StoryData d = Progress.d(p);
        String id = b.defId();
        if (active(d, id) == null) return;
        Fight f = FIGHTS.get(p.getUUID());
        if (f != null && f.boss.equals(id)) return;
        f = new Fight(id, p.server.getTickCount());
        FIGHTS.put(p.getUUID(), f);
        msg(p, "✠ Метка Элеоноры: " + BossManager.def(id).name() + " — " + rule(id, active(d, id)) + ".", ChatFormatting.AQUA);
        if (d.eleStage >= 2 && p.getRandom().nextFloat() < 0.25f) f.callAt = f.start + 300 + p.getRandom().nextInt(400);
    }

    public static void onZoneHit(ServerPlayer p) { fail(p, Type.CLEAN, "тебя задело красной зоной"); }
    public static void onEat(ServerPlayer p) { fail(p, Type.NOFOOD, "ты поел во время боя"); }

    private static void fail(ServerPlayer p, Type t, String why) {
        Fight f = FIGHTS.get(p.getUUID());
        if (f == null || f.failed != null || active(Progress.d(p), f.boss) != t) return;
        f.failed = why;
        msg(p, "✠ Метка сорвана: " + why + ". Попробуй в следующем бою.", ChatFormatting.RED);
    }

    public static void onDeath(ServerPlayer p) {
        Fight f = FIGHTS.remove(p.getUUID());
        if (f != null && f.ringUntil > 0) stopRing(p);
    }

    /** Раз в 10 тиков. */
    public static void tick(ServerPlayer p) {
        StoryData d = Progress.d(p);
        if (!d.eleMark.isEmpty() && d.eleMarkDay != day()) {
            d.eleMark = ""; p.setData(ModRegistry.STORY.get(), d);
            msg(p, "✠ Метка Элеоноры истекла — новый день, новые метки.", ChatFormatting.GRAY);
        }
        Fight f = FIGHTS.get(p.getUUID());
        if (f == null) return;
        long now = p.server.getTickCount();
        Type t = active(d, f.boss);
        if (t == null || now - f.start > 20 * 60 * 10) { FIGHTS.remove(p.getUUID()); return; }
        if (t == Type.STEADY && p.getHealth() < p.getMaxHealth() * 0.4f) fail(p, Type.STEADY, "здоровье упало ниже 40%");
        if (f.callAt > 0 && now >= f.callAt && !f.called && !DialogueManager.inDialogue(p) && !Vampire.jailed(p)) {
            f.called = true;
            f.ringUntil = now + 200;
            Phone.ringtone(p);
            PacketDistributor.sendToPlayer(p, new Network.PhoneData(3, "{\"sec\":10}"));
        }
        if (f.ringUntil > 0 && now > f.ringUntil) {
            f.ringUntil = -1;
            stopRing(p);
            msg(p, "📵 Пропущенный вызов: Элеонора.", ChatFormatting.GRAY);
            GameEvents.later(p.server, 30, () -> {
                Story.say(p, "Элеонора (СМС)", EleonoraEntity.COLOR, "Не берёшь трубку?! Ну и ладно. Позвоню Кармилле — пусть она проверит, где ты шляешься.");
                GameEvents.later(p.server, 50, () -> carmilla(p));
            });
        }
    }

    private static void stopRing(ServerPlayer p) {
        Phone.stopRingtone(p);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(4, "{}"));
    }

    private static void carmilla(ServerPlayer p) {
        if (!p.isAlive() || Vampire.jailed(p)) return;
        Fight f = FIGHTS.remove(p.getUUID());
        if (f != null && f.failed == null) msg(p, "✠ Метка сорвана: Кармилла увела тебя прямо с охоты.", ChatFormatting.RED);
        Vampire.ELE_CALL.add(p.getUUID());
        Vampire.abduct(p, Vampire.JAIL, false);
    }

    /** Ответить на звонок (клавиша телефона или ПКМ телефоном). true — звонок был. */
    public static boolean answer(ServerPlayer p) {
        Fight f = FIGHTS.get(p.getUUID());
        if (f == null || f.ringUntil < 0) return false;
        f.ringUntil = -1;
        stopRing(p);
        DialogueManager.open(p, null, call(p));
        return true;
    }

    private record Q(String text, String good, String bad1, String bad2) {}
    private static final Q[] QS = {
            new Q("Алло! Ты где? У тебя там что-то грохочет.", "Работаю. По твоей метке, между прочим.", "Отстань, я занят!", "А это кто?"),
            new Q("Слушай, как думаешь, мне пойдёт новый плащ? Тёмно-синий.", "Тебе всё идёт.", "Какой ещё плащ?", "Синий? Серьёзно?"),
            new Q("Я печенье испекла. С чесноком — от вампиров. Будешь?", "Конечно буду!", "Фу, с чесноком?", "Я на диете."),
            new Q("Ты ведь не общаешься с Кармиллой за моей спиной?", "Только по работе, честно.", "Ну… иногда.", "А что, нельзя?"),
            new Q("Ты кушал сегодня? Голос какой-то уставший.", "Да, всё хорошо, спасибо!", "Мне не до еды!", "А тебе какое дело?"),
            new Q("У меня под окном крыса. Огромная. С прессом. Это нормально?", "Не бойся, я разберусь.", "Это нормально.", "Покорми её."),
            new Q("Ой… я тебя не отвлекаю?", "Нет-нет, что ты!", "Вообще-то да!", "Отвлекаешь. Сильно."),
    };

    private static DialogueManager.Dialogue call(ServerPlayer p) {
        String W = "Элеонора 📞"; int C = EleonoraEntity.COLOR;
        List<Q> pool = new ArrayList<>(List.of(QS));
        Collections.shuffle(pool, new Random(p.getRandom().nextLong()));
        boolean[] bad = {false};
        DialogueManager.Dialogue dl = new DialogueManager.Dialogue("q0");
        for (int i = 0; i < 3; i++) {
            Q q = pool.get(i);
            String next = i == 2 ? "ok" : "q" + (i + 1);
            List<DialogueManager.Opt> os = new ArrayList<>(List.of(opt(q.good, next), opt(q.bad1, "bad"), opt(q.bad2, "bad")));
            Collections.shuffle(os, new Random(p.getRandom().nextLong()));
            dl.node("q" + i, W, C, q.text, os.toArray(new DialogueManager.Opt[0]));
        }
        dl.node("ok", W, C, "Ладно-ладно, не буду отвлекать. Удачи там! И не умирай, мне ещё печенье на тебя тратить.", pp -> {
                    StoryData d = Progress.d(pp); d.eleTokens += 1; pp.setData(ModRegistry.STORY.get(), d);
                    msg(pp, "✠ Элеонора довольна разговором: +1 жетон за вежливость.", ChatFormatting.AQUA);
                }, opt("[Положить трубку]", null))
          .node("bad", W, C, "…Ах вот как. Ну и ладно. Позвоню Кармилле — пусть она с тобой поговорит.", pp -> bad[0] = true, opt("[Гудки]", null))
          .onEnd(pp -> { if (bad[0]) GameEvents.later(pp.server, 40, () -> carmilla(pp)); })
          .onAbort(pp -> {
              Story.say(pp, "Элеонора (СМС)", C, "Ты бросил трубку?! Всё, звоню Кармилле.");
              GameEvents.later(pp.server, 40, () -> carmilla(pp));
          });
        return dl;
    }

    /** Босс убит — проверяем метку у каждого получившего награду. */
    public static void onBossKill(ServerPlayer p, String boss) {
        StoryData d = Progress.d(p);
        Type t = active(d, boss);
        Fight f = FIGHTS.remove(p.getUUID());
        if (t == null) return;
        if (f == null) { msg(p, "✠ Метка не засчитана: бой начался без тебя.", ChatFormatting.GRAY); return; }
        if (f.failed != null) return;
        if (t == Type.FAST && (p.server.getTickCount() - f.start) > FAST_S.getOrDefault(boss, 120) * 20L) {
            msg(p, "✠ Метка сорвана: не уложился в " + FAST_S.getOrDefault(boss, 120) + " секунд.", ChatFormatting.RED); return;
        }
        int r = reward(boss, t);
        d.eleTokens += r;
        d.eleMark = "";
        d.eleDone.putLong(boss, day());
        p.setData(ModRegistry.STORY.get(), d);
        Phone.chime(p);
        msg(p, "✠ Метка Элеоноры выполнена: «" + t.title + "»! +" + r + " жетонов охотницы. Загляни к ней.", ChatFormatting.GOLD);
    }

    // ------------------------------------------------------------------ разговор
    public static DialogueManager.Dialogue dialogue(ServerPlayer p, EleonoraEntity e) {
        StoryData d = Progress.d(p);
        String W = "Элеонора"; int C = EleonoraEntity.COLOR;
        DialogueManager.Dialogue dl = new DialogueManager.Dialogue("a");
        dl.node("a", W, C, "Охотник. Жетонов у тебя: " + d.eleTokens + ". Чем займёмся?",
                opt("Метки на сегодня", "m"), opt("Лавка охотницы", "shop"), opt("Как работают метки?", "how"), opt("[Уйти]", null));
        dl.node("how", W, C, "Каждый день я помечаю тварей, которых ты уже бил. Условие у каждой метки своё — быстро, чисто, без еды, без паники. "
                + "Выполнишь — жетоны. Их не обесценит никакая инфляция: что заработал, то и твоё. И да, я иногда звоню во время охоты. Бери трубку.", opt("Понял", "a"));
        dl.node("shop", W, C, "Смотри. Серебро руками не трогай.", pp -> GameEvents.later(pp.server, 2, () -> openShop(pp, e)), opt("[Смотреть]", null));
        Type cur = null; String curBoss = null;
        if (!d.eleMark.isEmpty()) { curBoss = d.eleMark.substring(0, d.eleMark.indexOf(':')); cur = active(d, curBoss); }
        if (cur != null) {
            BossManager.Def bd = BossManager.def(curBoss);
            dl.node("m", W, C, "Твоя метка: " + bd.name() + " — «" + cur.title + "»: " + rule(curBoss, cur) + ". Награда: " + reward(curBoss, cur) + " жет.",
                    opt("Сбросить метку", "drop"), opt("Иду на охоту", null));
            dl.node("drop", W, C, "Сдаёшься? Ладно. Сегодня эту тварь я больше не помечу.", pp -> {
                StoryData dd = Progress.d(pp); dd.eleDone.putLong(dd.eleMark.substring(0, dd.eleMark.indexOf(':')), day()); dd.eleMark = "";
                pp.setData(ModRegistry.STORY.get(), dd);
            }, opt("[Уйти]", null));
            return dl;
        }
        List<DialogueManager.Opt> os = new ArrayList<>();
        for (BossManager.Def bd : BossManager.DEFS) {
            if ((d.trophies & bd.trophyBit()) == 0 || d.eleDone.getLong(bd.id()) == day()) continue;
            Type t = today(bd.id());
            String id = "take_" + bd.id();
            os.add(opt(bd.name() + ": «" + t.title + "» (+" + reward(bd.id(), t) + ")", id));
            dl.node(id, W, C, bd.name() + " — «" + t.title + "»: " + rule(bd.id(), t) + ". Справишься — " + reward(bd.id(), t) + " жетонов.", pp -> {
                StoryData dd = Progress.d(pp); dd.eleMark = bd.id() + ":" + t.name(); dd.eleMarkDay = day(); pp.setData(ModRegistry.STORY.get(), dd);
                msg(pp, "✠ Метка взята: " + bd.name() + " — " + rule(bd.id(), t) + ".", ChatFormatting.AQUA);
            }, opt("Беру", null));
        }
        os.add(opt("Назад", "a"));
        dl.node("m", W, C, os.size() == 1 ? "Сегодня меток нет: либо ты всех уже сделал, либо ещё никого не победил. Возвращайся завтра."
                : "Вот кого я пометила сегодня. Одна метка за раз.", os.toArray(new DialogueManager.Opt[0]));
        return dl;
    }

    private static void openShop(ServerPlayer p, EleonoraEntity e) {
        DialogueManager.forget(p);
        Shop.VENDOR.put(p.getUUID(), "eleonora");
        p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new com.pvpciv.menu.TraderMenu(id, inv), Component.literal("Элеонора")));
        Shop.sendOffers(p);
    }

    /** Для тестов: звонок прямо сейчас (бой-заглушка с первой меткой дня). */
    public static void testCall(ServerPlayer p) {
        Fight f = new Fight("brute", p.server.getTickCount());
        f.callAt = p.server.getTickCount() + 20;
        FIGHTS.put(p.getUUID(), f);
        StoryData d = Progress.d(p);
        if (active(d, "brute") == null) { d.eleMark = "brute:" + today("brute").name(); d.eleMarkDay = day(); p.setData(ModRegistry.STORY.get(), d); }
    }

    public static void forget(ServerPlayer p) { FIGHTS.remove(p.getUUID()); }

    private static void msg(ServerPlayer p, String s, ChatFormatting c) { p.sendSystemMessage(Component.literal(s).withStyle(c)); }
}
