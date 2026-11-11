package com.pvpciv.entity.boss;

import com.pvpciv.combat.CombatEvents;
import com.pvpciv.combat.Mobs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Хранительница Склепа (ур. 8), Ледяной Склеп. Умения:
 *  • Аура холода — кто стоит ближе 5.5 блока, каждые 2 с получает стак холода (5 стаков — заморозка).
 *  • Ледяные шипы — дорожка голубых кругов от неё к цели, взрываются по очереди (в ярости — три дорожки веером).
 *  • Ледяная волна — вонзает копьё в землю: круг R6.5 + ударная волна до 12 блоков (перепрыгни).
 *  • Копья с неба (в ярости) — круги под каждым игроком и вокруг, через 1.4 с падают ледяные копья.
 *  • Скольжение — если кто-то трётся вплотную 3 с, исчезает в снежном вихре и появляется в стороне.
 *  • На 50% HP — призывает двух обмороженных и входит в ярость.
 */
public class KeeperBoss extends CivBoss {
    private static final BlockParticleOption ICE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState());
    private int closeTicks;

    public KeeperBoss(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Override public String defId() { return "keeper"; }
    @Override public String model() { return "crypt_keeper"; }
    @Override public float renderScale() { return 1.3f; }
    @Override protected String[] actions() { return new String[]{"attack", "cast", "raise", "slam", "blink"}; }

    @Override
    protected void passive(ServerLevel lvl) {
        if (tickCount % 40 == 0) {
            for (int i = 0; i < 28; i++) {
                double a = i * Math.PI / 14;
                lvl.sendParticles(ParticleTypes.SNOWFLAKE, getX() + Math.cos(a) * 5.5, getY() + 0.3, getZ() + Math.sin(a) * 5.5, 1, 0, 0.05, 0, 0);
            }
            for (ServerPlayer p : victims(lvl, 5.5)) CombatEvents.frost(p, lvl);
        }
        // скольжение от тех, кто трётся вплотную
        boolean close = !victims(lvl, 2.6).isEmpty();
        closeTicks = close ? closeTicks + 1 : Math.max(0, closeTicks - 2);
        if (closeTicks > 60 && castTicks <= 0) { closeTicks = 0; blink(lvl); }
    }

    @Override
    protected void ability(ServerLevel lvl, ServerPlayer target) {
        int roll = random.nextInt(10);
        if (enraged && roll < 4) spears(lvl);
        else if (distanceTo(target) < 7 && roll < 7) nova(lvl);
        else spikes(lvl, target);
        cooldown = enraged ? 55 + random.nextInt(25) : 90 + random.nextInt(35);
    }

    private void spikes(ServerLevel lvl, ServerPlayer target) {
        play("cast", 30);
        faceTo(target.position());
        lvl.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 2f, 1.4f);
        double base = Math.atan2(target.getZ() - getZ(), target.getX() - getX());
        double[] spread = enraged ? new double[]{-0.45, 0, 0.45} : new double[]{0};
        for (double s : spread) {
            double a = base + s;
            for (int i = 0; i < 10; i++) {
                Vec3 at = position().add(Math.cos(a) * (2 + i * 1.6), 0, Math.sin(a) * (2 + i * 1.6));
                zone(lvl, at, 1.3, 18 + i * 2, CYAN).onHit(p -> {
                    p.hurt(damageSources().mobAttack(this), dmg() * 1.1f);
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                    knock(p, at, 0.1, 0.7);
                }).fx(this::spikeFx);
            }
        }
    }

    private void spikeFx(Vec3 c) {
        if (!(level() instanceof ServerLevel lvl)) return;
        lvl.playSound(null, c.x, c.y, c.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.8f, 0.7f + random.nextFloat() * 0.4f);
        lvl.sendParticles(ICE, c.x, c.y + 0.5, c.z, 25, 0.35, 0.9, 0.35, 0.1);
        lvl.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y + 1.0, c.z, 10, 0.3, 0.8, 0.3, 0.02);
    }

    private void nova(ServerLevel lvl) {
        play("slam", 30);
        castYaw = getYRot();
        lvl.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2f, 1.2f);
        Vec3 c = position();
        zone(lvl, c, 6.5, 20, CYAN).onHit(p -> {
            p.hurt(damageSources().mobAttack(this), dmg() * 1.3f);
            CombatEvents.frost(p, lvl);
            CombatEvents.frost(p, lvl);
            knock(p, c, 0.8, 0.6);
        }).fx(at -> {
            lvl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2f, 0.5f);
            lvl.sendParticles(ICE, at.x, at.y + 0.3, at.z, 120, 3, 0.3, 3, 0.2);
            lvl.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 0.5, at.z, 60, 3, 0.5, 3, 0.05);
            wave(lvl, at, 12, 0.4, dmg() * 0.7f, WHITE);
        });
    }

    private void spears(ServerLevel lvl) {
        play("raise", 34);
        castYaw = getYRot();
        lvl.playSound(null, blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 2f, 0.8f);
        for (ServerPlayer p : victims(lvl, 24)) spear(lvl, p.position().add(p.getDeltaMovement().scale(8)));
        for (int i = 0; i < 5; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = 3 + random.nextDouble() * 8;
            spear(lvl, (home != null ? home : position()).add(Math.cos(a) * r, 0, Math.sin(a) * r));
        }
    }

    private void spear(ServerLevel lvl, Vec3 at) {
        CivBoss.Zone z = zone(lvl, at, 2.0, 28, CYAN);
        later(16, () -> { for (int k = 0; k < 6; k++) lvl.sendParticles(ParticleTypes.END_ROD, z.c.x, z.c.y + 9 - k * 1.2, z.c.z, 1, 0.05, 0.1, 0.05, 0); });
        z.onHit(p -> {
            p.hurt(damageSources().mobAttack(this), dmg() * 1.25f);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }).fx(c -> {
            lvl.playSound(null, c.x, c.y, c.z, SoundEvents.TRIDENT_HIT_GROUND, SoundSource.HOSTILE, 1.5f, 0.6f);
            lvl.playSound(null, c.x, c.y, c.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1f, 1.2f);
            lvl.sendParticles(ICE, c.x, c.y + 0.6, c.z, 40, 0.6, 1.2, 0.6, 0.15);
            lvl.sendParticles(ParticleTypes.ITEM_SNOWBALL, c.x, c.y + 0.4, c.z, 20, 0.8, 0.3, 0.8, 0.1);
        });
    }

    private void blink(ServerLevel lvl) {
        Vec3 old = position();
        Vec3 c = home != null ? home : old;
        triggerAnim("main", "blink");
        lvl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5f, 1.4f);
        lvl.sendParticles(ParticleTypes.SNOWFLAKE, old.x, old.y + 1.2, old.z, 60, 0.5, 1.0, 0.5, 0.08);
        double a = random.nextDouble() * Math.PI * 2;
        teleportTo(c.x + Math.cos(a) * 8, c.y, c.z + Math.sin(a) * 8);
        lvl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1.2, getZ(), 60, 0.5, 1.0, 0.5, 0.08);
        zone(lvl, old, 3.0, 15, CYAN).onHit(p -> {
            p.hurt(damageSources().mobAttack(this), dmg());
            CombatEvents.frost(p, lvl);
        }).fx(this::spikeFx);
        cooldown = Math.min(cooldown, 20);
    }

    @Override
    protected void onEnrage(ServerLevel lvl) {
        play("raise", 34);
        castYaw = getYRot();
        lvl.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_WOLOLO, SoundSource.HOSTILE, 3f, 0.6f);
        Vec3 c = home != null ? home : position();
        for (int i = 0; i < 2; i++) {
            double a = i * Math.PI + 0.7;
            Mobs.spawn(lvl, Mobs.Kind.FROST_ZOMBIE, 7, false, c.add(Math.cos(a) * 5, 0, Math.sin(a) * 5), "floor1b_mob", "");
        }
        cooldown = 40;
        for (ServerPlayer p : victims(lvl, 40))
            p.sendSystemMessage(Component.literal("❄ «Склеп не отпустит тебя.» Хранительница призывает стражу — с неба падают копья!").withStyle(ChatFormatting.AQUA));
    }
}
