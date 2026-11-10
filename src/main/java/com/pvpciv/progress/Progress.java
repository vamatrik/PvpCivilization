package com.pvpciv.progress;

import com.pvpciv.PvpCiv;
import com.pvpciv.data.StoryData;
import com.pvpciv.game.Story;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Уровень, опыт и Искры игрока. 30 уровней на мир, 3 на этаж. */
public final class Progress {
    private static final ResourceLocation HP_ID = PvpCiv.id("level_hp");

    public static StoryData d(ServerPlayer p) { return p.getData(ModRegistry.STORY.get()); }
    private static void save(ServerPlayer p, StoryData d) { p.setData(ModRegistry.STORY.get(), d); }

    public static long xpToNext(int level) { return Math.round(60 * Math.pow(1.22, level - 1)); }

    /** Опыт за моба уровня mobLevel с учётом разницы уровней. */
    public static long xpFor(int playerLevel, int mobLevel, double mult) {
        double base = 6 * Math.pow(1.15, mobLevel - 1) * mult;
        int below = playerLevel - mobLevel;
        double k = below <= 2 ? 1.0 : below == 3 ? 0.5 : below <= 5 ? 0.2 : 0.05;
        return Math.max(1, Math.round(base * k));
    }

    public static void addXp(ServerPlayer p, long amount) {
        StoryData d = d(p);
        d.xp += amount;
        boolean up = false;
        while (d.xp >= xpToNext(d.level)) {
            d.xp -= xpToNext(d.level);
            d.level++;
            up = true;
        }
        save(p, d);
        if (up) {
            applyStats(p);
            p.setHealth(p.getMaxHealth());
            Story.title(p, "УРОВЕНЬ " + d.level, "+1 ❤ к здоровью, +1% к урону", 0xFFD166);
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.1f);
            p.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 40, 0.5, 1, 0.5, 0.2);
        }
        Story.sync(p);
    }

    public static void addMoney(ServerPlayer p, long amount, boolean notify) {
        StoryData d = d(p);
        d.money = Math.max(0, d.money + amount);
        save(p, d);
        if (notify && amount > 0) {
            p.displayClientMessage(Component.literal("+" + amount + " ✦").withStyle(ChatFormatting.GOLD), true);
            p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4f, 1.6f);
        }
        Story.sync(p);
    }

    /** +1 сердце за каждый уровень после первого. */
    public static void applyStats(ServerPlayer p) {
        AttributeInstance hp = p.getAttribute(Attributes.MAX_HEALTH);
        if (hp == null) return;
        hp.removeModifier(HP_ID);
        int lvl = d(p).level;
        if (lvl > 1) hp.addPermanentModifier(new AttributeModifier(HP_ID, 2.0 * (lvl - 1), AttributeModifier.Operation.ADD_VALUE));
        Titles.apply(p);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** Штраф смерти: деньги и половина опыта до следующего уровня. Возвращает потерянные Искры. */
    public static long deathPenalty(ServerPlayer p, double moneyFraction) {
        StoryData d = d(p);
        long lost = (long) Math.floor(d.money * moneyFraction);
        d.money -= lost;
        d.xp = d.xp / 2;
        save(p, d);
        return lost;
    }
}
