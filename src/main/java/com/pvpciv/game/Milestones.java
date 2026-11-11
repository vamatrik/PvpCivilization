package com.pvpciv.game;

import com.pvpciv.data.StoryData;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import static com.pvpciv.game.DialogueManager.opt;

/**
 * «Дневник подвигов» Тимофея: счётчики копятся сами, вехи сдаются у него — за каждую маленький вечный бонус,
 * за последнюю ступень — титул.
 */
public final class Milestones {
    public enum Track {
        HITS("Удары", new long[]{100, 500, 2000, 5000, 10000, 25000}, "+0.5% к урону", "hammer_hands"),
        KM("Пройдено, км", new long[]{1, 5, 15, 50, 100}, "+0.5% к скорости", "marathon"),
        KILLS("Убито мобов", new long[]{50, 250, 1000, 5000}, "+1 к здоровью", "exterminator"),
        STASH("Тайников открыто", new long[]{10, 50, 150}, "+1 к здоровью", null),
        BITES("Укусов Кармиллы", new long[]{1, 5, 20}, "+1 к здоровью", "donor");
        public final String title, bonus, titleId; public final long[] steps;
        Track(String t, long[] s, String b, String tid) { title = t; steps = s; bonus = b; titleId = tid; }
    }

    public static long value(StoryData d, Track t) {
        return switch (t) {
            case HITS -> d.hits;
            case KM -> d.distM / 1000;
            case KILLS -> { long n = 0; for (String k : d.kills.getAllKeys()) n += d.kills.getInt(k); yield n; }
            case STASH -> d.stashesOpened;
            case BITES -> d.jailed;
        };
    }

    public static int claimed(StoryData d, Track t) { return d.milestones.getInt(t.name()); }

    public static DialogueManager.Dialogue dialogue(ServerPlayer p) {
        StoryData d = Progress.d(p);
        StringBuilder sb = new StringBuilder("Дневник подвигов! ");
        int ready = 0;
        for (Track t : Track.values()) {
            int c = claimed(d, t);
            long v = value(d, t);
            String next = c >= t.steps.length ? "всё" : String.valueOf(t.steps[c]);
            sb.append(t.title).append(": ").append(v).append("/").append(next).append(". ");
            for (int i = c; i < t.steps.length && v >= t.steps[i]; i++) ready++;
        }
        DialogueManager.Dialogue dl = new DialogueManager.Dialogue("a");
        if (ready > 0) dl.node("a", "Тимофей Глыба", 0xE05050, sb + "О, есть что отметить — " + ready + "!", opt("Забрать награды", "claim"), opt("Позже", null))
                .node("claim", "Тимофей Глыба", 0xE05050, "Вот это я понимаю — прогресс! Мышцы помнят, а я записываю.", Milestones::claimAll, opt("[Уйти]", null));
        else dl.node("a", "Тимофей Глыба", 0xE05050, sb + "Пока нечего отмечать. Бей, ходи, убивай — счётчики идут сами.", opt("[Уйти]", null));
        return dl;
    }

    private static void claimAll(ServerPlayer p) {
        StoryData d = Progress.d(p);
        for (Track t : Track.values()) {
            int c = claimed(d, t);
            long v = value(d, t);
            while (c < t.steps.length && v >= t.steps[c]) {
                c++;
                p.sendSystemMessage(Component.literal("✦ Веха «" + t.title + " " + t.steps[c - 1] + "»: " + t.bonus).withStyle(ChatFormatting.GOLD));
                if (c == t.steps.length && t.titleId != null) com.pvpciv.progress.Titles.grant(p, t.titleId);
            }
            d.milestones.putInt(t.name(), c);
        }
        p.setData(ModRegistry.STORY.get(), d);
        apply(p);
        Story.sync(p);
    }

    /** Вечные бонусы вех (вход в игру, после сдачи). */
    public static void apply(ServerPlayer p) {
        StoryData d = Progress.d(p);
        mod(p, Attributes.ATTACK_DAMAGE, "ms_dmg", 0.005 * claimed(d, Track.HITS), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        mod(p, Attributes.MOVEMENT_SPEED, "ms_speed", 0.005 * claimed(d, Track.KM), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        mod(p, Attributes.MAX_HEALTH, "ms_hp", claimed(d, Track.KILLS) + claimed(d, Track.STASH) + claimed(d, Track.BITES), AttributeModifier.Operation.ADD_VALUE);
    }

    private static void mod(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a, String id, double v, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        ResourceLocation rl = com.pvpciv.PvpCiv.id(id);
        inst.removeModifier(rl);
        if (v != 0) inst.addPermanentModifier(new AttributeModifier(rl, v, op));
    }
}
