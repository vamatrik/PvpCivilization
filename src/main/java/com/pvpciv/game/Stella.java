package com.pvpciv.game;

import com.pvpciv.data.StoryData;
import com.pvpciv.inv.Bag;
import com.pvpciv.item.PhaseAnchorItem;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModF4;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import static com.pvpciv.game.DialogueManager.opt;

/**
 * 0.11 — Стелла, космическая эльфийка-инженер (вестибюль NaZa). Её корабль разбился о Шпиль; ей нужно откалибровать
 * «Фазовый якорь»: 1000 рывков с прибором в инвентаре. Награда — навык «Рывок II» (КД 6 с), прибор остаётся трофеем.
 */
public final class Stella {
    private Stella() {}
    public static final int C = 0xC060FF;
    public static final String NAME = "Стелла";

    /** Стадии: 0 — не знакомы, 1 — прибор выдан, 2 — сдан (Рывок II). */
    public static int stage(StoryData d) { return d.gates.getInt("stella_q"); }

    public static ItemStack device(ServerPlayer p) {
        var h = Bag.get(p);
        for (int i = 0; i < h.getSlots(); i++) if (h.getStackInSlot(i).getItem() instanceof PhaseAnchorItem) return h.getStackInSlot(i);
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).getItem() instanceof PhaseAnchorItem) return p.getInventory().getItem(i);
        return ItemStack.EMPTY;
    }

    /** Каждый рывок с прибором — +1 заряд. */
    public static void onDash(ServerPlayer p) {
        ItemStack st = device(p);
        if (st.isEmpty()) return;
        int c = PhaseAnchorItem.charge(st);
        if (c >= PhaseAnchorItem.NEED) return;
        PhaseAnchorItem.setCharge(st, ++c);
        if (c % 100 == 0 && c < PhaseAnchorItem.NEED) p.displayClientMessage(Component.literal("⟁ Фазовый якорь: " + c + "/" + PhaseAnchorItem.NEED).withStyle(x -> x.withColor(C)), true);
        if (c == PhaseAnchorItem.NEED) {
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.5f);
            Story.title(p, "Фазовый якорь заряжен", "Отнеси его Стелле в вестибюль NaZa", C);
        }
    }

    public static void talk(ServerPlayer p, com.pvpciv.entity.AnimeNpc npc) {
        StoryData d = Progress.d(p);
        DialogueManager.Dialogue dl = new DialogueManager.Dialogue("a");
        int st = stage(d);
        ItemStack dev = device(p);
        if (st == 0) {
            dl.node("a", NAME, C, "О! Живой. Не робот, не охранник и даже не растение. Я Стелла. Да, уши настоящие. Нет, я не отсюда — мой корабль сейчас торчит из обшивки Шпиля где-то над нами.",
                            opt("Как ты сюда попала?", "b"), opt("[Уйти]", null))
              .node("b", NAME, C, "Фазовый прыжок пошёл… не туда. Чтобы откалибровать навигатор, мне нужен «Фазовый якорь», заряженный реальными рывками живого существа. Тысячей рывков. Сама не могу — я инженер, а не спринтер.",
                            opt("Тысяча рывков? Давай прибор", "c"), opt("Звучит утомительно", null))
              .node("c", NAME, C, "Держи. Просто носи его с собой и делай рывки — он сам считает. Когда заряд будет полный, приходи: научу тебя рывку, который у нас в академии называют «вторым». Перезарядка короче, летишь дальше.", pp -> {
                  StoryData dd = Progress.d(pp);
                  dd.gates.putInt("stella_q", 1);
                  pp.setData(ModRegistry.STORY.get(), dd);
                  Bag.give(pp, new ItemStack(ModF4.PHASE_ANCHOR.get()));
                  pp.sendSystemMessage(Component.literal("Квест «Тысяча рывков»: сделай 1000 рывков с Фазовым якорем при себе.").withStyle(ChatFormatting.LIGHT_PURPLE));
              }, opt("[Взять прибор]", null));
        } else if (st == 1 && !dev.isEmpty() && PhaseAnchorItem.charged(dev)) {
            dl.node("a", NAME, C, "Он светится! Тысяча ровно, я чувствую резонанс. Стой смирно… вот так. Теперь твоё тело помнит фазовый рывок. А якорь оставь себе — навигатор я уже откалибровала по нему.",
                            pp -> finish(pp), opt("Спасибо, Стелла", null));
        } else if (st == 1 && dev.isEmpty()) {
            dl.node("a", NAME, C, "Ты потерял якорь?! Ладно… у меня был запасной. Последний! Совсем последний.", pp -> Bag.give(pp, new ItemStack(ModF4.PHASE_ANCHOR.get())), opt("Прости", null));
        } else if (st == 1) {
            dl.node("a", NAME, C, "Заряд " + PhaseAnchorItem.charge(dev) + " из " + PhaseAnchorItem.NEED + ". Бегай-бегай! Рывок в бою считается тоже, если что.", opt("Бегу", null));
        } else {
            String[] hi = {"Навигатор почти готов. Ещё пара дней — и я улечу. Наверное. Если NaZa не украдёт мне двигатель.",
                    "Сорока на втором этаже спрашивала про якорь. Видимо, на чёрном рынке за него дают кучу искр. Это твой трофей — решай сам.",
                    "Знаешь, что значит NaZa? Они говорят — «Научно-Заводской Анклав». Я видела старую табличку: «НАЗАд дороги нет». Смешно. Наверное."};
            dl.node("a", NAME, C, hi[p.getRandom().nextInt(hi.length)], opt("[Уйти]", null));
        }
        DialogueManager.open(p, npc, dl);
    }

    private static void finish(ServerPlayer p) {
        StoryData d = Progress.d(p);
        d.gates.putInt("stella_q", 2);
        d.gates.putBoolean("dash2", true);
        if (d.skill1.isEmpty() || d.skill1.equals("dash")) d.skill1 = "dash2";
        p.setData(ModRegistry.STORY.get(), d);
        Progress.addXp(p, Progress.xpFor(d.level, 20, 30));
        Story.title(p, "Рывок II", "Перезарядка 6 с · летишь дальше", C);
        com.pvpciv.progress.Titles.grant(p, "phase");
        Skills.sync(p);
    }
}
