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

/** Уровень, опыт и Искры игрока. 50 уровней на мир, 5 на этаж (с 0.7). */
public final class Progress {
    private static final ResourceLocation HP_ID = PvpCiv.id("level_hp");

    public static StoryData d(ServerPlayer p) { return p.getData(ModRegistry.STORY.get()); }
    private static void save(ServerPlayer p, StoryData d) { p.setData(ModRegistry.STORY.get(), d); }

    public static long xpToNext(int level) { return Math.round(150 * Math.pow(1.12, level - 1)); }

    /** Опыт за моба уровня mobLevel с учётом разницы уровней. */
    public static long xpFor(int playerLevel, int mobLevel, double mult) {
        double base = 6 * Math.pow(1.08, mobLevel - 1) * mult;
        int below = playerLevel - mobLevel;
        double k = below <= 3 ? 1.0 : below <= 5 ? 0.5 : below <= 8 ? 0.2 : 0.05;
        return Math.max(1, Math.round(base * k));
    }

    /** Опыт за обычного моба. */
    public static long xpForMob(int playerLevel, int mobLevel, double mult) {
        double base = 6 * Math.pow(1.08, mobLevel - 1) * mult;
        int below = playerLevel - mobLevel;
        // бета 0.9.8: моб ниже на 1 → −50%, на 2 → −75%, на 3+ → опыта нет
        double k = below <= 0 ? 1.0 : below == 1 ? 0.5 : below == 2 ? 0.25 : 0.0;
        if (k <= 0) return 0;
        return Math.max(1, Math.round(base * k));
    }

    /** Потолок уровня, пока не открыт следующий этаж: «уровень этажа» (сильнейший босс) + 3. Этаж 1 → 11, этаж 2 → 16. */
    /** Потолок уровня: макс. уровень мобов открытого этажа + 3 (0.11: этаж 3 — 19, этаж 4 — 27). */
    public static int levelCap(StoryData d) { return com.pvpciv.game.SkyLift.f4Ok(d) ? 24 + 3 : com.pvpciv.game.SkyLift.f3Ok(d) ? 16 + 3 : d.liftOk ? 13 + 3 : 8 + 3; }

    public static void addXp(ServerPlayer p, long amount) {
        StoryData d = d(p);
        int cap = levelCap(d);
        if (d.level >= cap) {
            if (d.xp < xpToNext(d.level) - 1) d.xp = Math.min(d.xp + amount, xpToNext(d.level) - 1);
            else if (p.tickCount % 200 < 5) p.displayClientMessage(Component.literal("Потолок уровня " + cap + " для этого этажа — открой следующий этаж, чтобы расти дальше.").withStyle(ChatFormatting.GRAY), true);
            save(p, d); Story.sync(p);
            return;
        }
        d.xp += amount;
        boolean up = false;
        while (d.xp >= xpToNext(d.level) && d.level < cap) {
            d.xp -= xpToNext(d.level);
            d.level++;
            up = true;
        }
        save(p, d);
        if (up) {
            applyStats(p);
            p.setHealth(p.getMaxHealth());
            Story.title(p, "УРОВЕНЬ " + d.level, "+" + (int) (levelHp(d.level) - levelHp(d.level - 1)) + " здоровья, +1.5% к урону", 0xFFD166);
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

    /** Бонус HP за уровни: ур. 2 даёт +3, каждый следующий — на 0.2 больше (ур. 8 → +25, ур. 16 → +66). */
    public static double levelHp(int lvl) {
        return Math.round(com.pvpciv.combat.Balance.playerHp(lvl) - 20);   // 0.9.9: своя шкала (ур.1 — 30, ур.10 — 132, ур.16 — 360)
    }

    /** HP и урон от уровня. */
    public static void applyStats(ServerPlayer p) {
        AttributeInstance hp = p.getAttribute(Attributes.MAX_HEALTH);
        if (hp == null) return;
        hp.removeModifier(HP_ID);
        int lvl = d(p).level;
        hp.addPermanentModifier(new AttributeModifier(HP_ID, levelHp(lvl), AttributeModifier.Operation.ADD_VALUE));
        Titles.apply(p);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** Штраф смерти: деньги и 15% опыта до следующего уровня. Возвращает потерянные Искры. */
    public static long deathPenalty(ServerPlayer p, double moneyFraction) {
        StoryData d = d(p);
        long lost = (long) Math.floor(d.money * moneyFraction);
        d.money -= lost;
        d.xp = Math.round(d.xp * 0.85);   // бета 12.11: при смерти теряется 15% опыта (было 50%)
        save(p, d);
        return lost;
    }
}
