package com.pvpciv.entity.boss;

import com.pvpciv.combat.Mobs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Дон Витторио (ур. 10), клуб «Серра». Первый враг с огнестрелом.
 *  • Очередь — три красные дорожки веером к цели, через 0.9 с по ним проходит свинец.
 *  • Граната — круги под игроками, через 1.5 с взрыв.
 *  • Свистнуть своих — зовёт гопников (не больше трёх рядом).
 *  • В ярости (50%) — «Ливень свинца»: крутится на месте, дорожки обходят круг по часовой.
 */
public class DonBoss extends CivBoss {
    private int callCd = 200;

    public DonBoss(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Override public String defId() { return "don"; }
    @Override public String model() { return "don"; }
    @Override public float renderScale() { return 1.25f; }
    @Override protected String[] actions() { return new String[]{"attack", "burst", "spray", "grenade", "call"}; }

    @Override
    protected void passive(ServerLevel lvl) {
        if (--callCd <= 0 && castTicks <= 0) {
            callCd = 500;
            int near = lvl.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(blockPosition()).inflate(20), m -> m.getTags().contains("don_goon")).size();
            if (near < 3) call(lvl, 2);
        }
    }

    @Override
    protected void ability(ServerLevel lvl, ServerPlayer target) {
        int roll = random.nextInt(10);
        if (enraged && roll < 3) spray(lvl);
        else if (roll < 6) burst(lvl, target);
        else grenades(lvl);
        cooldown = enraged ? 55 + random.nextInt(25) : 85 + random.nextInt(30);
    }

    private void line(ServerLevel lvl, double yaw, int delay, float dmgK) {
        Vec3 from = position();
        for (int i = 1; i <= 11; i++) {
            Vec3 at = from.add(Math.cos(yaw) * i * 1.5, 0, Math.sin(yaw) * i * 1.5);
            zone(lvl, at, 0.9, delay + i / 4, RED).onHit(p -> p.hurt(damageSources().mobAttack(this), dmg() * dmgK))
                    .fx(c -> lvl.sendParticles(ParticleTypes.CRIT, c.x, c.y + 1.1, c.z, 2, 0.1, 0.1, 0.1, 0.1));
        }
        later(delay, () -> {
            lvl.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1.5f, 1.8f);
            lvl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.6, getZ(), 6, 0.2, 0.1, 0.2, 0.02);
        });
    }

    private void burst(ServerLevel lvl, ServerPlayer target) {
        play("burst", 30);
        faceTo(target.position());
        double yaw = Math.atan2(target.getZ() - getZ(), target.getX() - getX());
        lvl.playSound(null, blockPosition(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.HOSTILE, 1.5f, 0.7f);
        for (double s : enraged ? new double[]{-0.4, -0.2, 0, 0.2, 0.4} : new double[]{-0.22, 0, 0.22}) line(lvl, yaw + s, 18, 0.7f);
    }

    private void spray(ServerLevel lvl) {
        play("spray", 52);
        castYaw = Float.NaN;
        lvl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5f, 1.4f);
        for (ServerPlayer p : victims(lvl, 40)) p.sendSystemMessage(Component.literal("☠ «Ливень свинца!» — уходи за спину Дона или за колонну!").withStyle(ChatFormatting.GOLD));
        double base = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < 16; i++) line(lvl, base + i * Math.PI / 8, 10 + i * 3, 0.6f);
    }

    private void grenades(ServerLevel lvl) {
        play("grenade", 24);
        castYaw = getYRot();
        lvl.playSound(null, blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 1.5f, 0.5f);
        int n = 0;
        for (ServerPlayer p : victims(lvl, 24)) {
            if (n++ >= 4) break;
            Vec3 at = p.position().add(p.getDeltaMovement().scale(6));
            later(10, () -> lvl.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.3, at.z, 10, 0.2, 0.1, 0.2, 0.01));
            zone(lvl, at, 3.0, 30, RED).onHit(v -> { v.hurt(damageSources().mobAttack(this), dmg() * 1.3f); knock(v, at, 0.8, 0.6); })
                    .fx(c -> {
                        lvl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2f, 1.1f);
                        lvl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 3, 0.8, 0.2, 0.8, 0);
                    });
        }
    }

    private void call(ServerLevel lvl, int n) {
        play("call", 30);
        lvl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.HOSTILE, 2f, 0.6f);
        Vec3 c = home != null ? home : position();
        for (int i = 0; i < n; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            var m = Mobs.spawn(lvl, Mobs.Kind.THUG, 9, false, c.add(Math.cos(a) * 9, 0, Math.sin(a) * 9), "floor2_mob", "don_goon");
            if (m != null) lvl.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + 1, m.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
        }
    }

    @Override
    protected void onEnrage(ServerLevel lvl) {
        call(lvl, 3);
        cooldown = 40;
        for (ServerPlayer p : victims(lvl, 40))
            p.sendSystemMessage(Component.literal("☠ «Ну всё. Ребята — работаем.» Дон зовёт всю бригаду и достаёт второй магазин.").withStyle(ChatFormatting.GOLD));
    }
}
