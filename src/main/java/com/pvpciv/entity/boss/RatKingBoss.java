package com.pvpciv.entity.boss;

import com.pvpciv.combat.Mobs;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Крысиный король (ур. 13), Тронный сток. Да, у него пресс.
 *  • Дудка — зовёт крыс (не больше 8 рядом).
 *  • Ядовитые лужи — зелёные круги под игроками; после удара лужа травит ещё 6 с.
 *  • Подкоп — уходит под пол и выныривает в другом месте: круг R4 под ним (подбрасывает).
 *  • Удар хвостом — если рядом толпятся: круг R4.5 вокруг себя.
 *  • В ярости — огромная стая и подкопы чаще.
 */
public class RatKingBoss extends CivBoss {
    private int hidden;

    public RatKingBoss(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Override public String defId() { return "rat_king"; }
    @Override public String model() { return "rat_king"; }
    @Override public float renderScale() { return 1.35f; }
    @Override protected String[] actions() { return new String[]{"attack", "summon", "burrow", "emerge", "sweep"}; }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource src, float amount) {
        if (hidden > 0 && !src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(src, amount);
    }

    @Override
    protected void passive(ServerLevel lvl) {
        if (hidden > 0 && --hidden == 0) setInvisible(false);
    }

    @Override
    protected void ability(ServerLevel lvl, ServerPlayer target) {
        int close = victims(lvl, 4.5).size();
        int roll = random.nextInt(10);
        if (close >= 1 && roll < 3) sweep(lvl);
        else if (roll < (enraged ? 5 : 4)) burrow(lvl, target);
        else if (roll < 7) puddles(lvl);
        else summon(lvl, enraged ? 4 : 3);
        cooldown = enraged ? 50 + random.nextInt(25) : 80 + random.nextInt(30);
    }

    private void summon(ServerLevel lvl, int n) {
        int near = lvl.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(blockPosition()).inflate(20), m -> m.getTags().contains("king_rat")).size();
        play("summon", 30);
        castYaw = getYRot();
        lvl.playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.HOSTILE, 2f, 0.5f);
        Vec3 c = home != null ? home : position();
        for (int i = 0; i < Math.min(n, 8 - near); i++) {
            double a = random.nextDouble() * Math.PI * 2;
            var m = Mobs.spawn(lvl, Mobs.Kind.RAT, 12, false, c.add(Math.cos(a) * 8, 0, Math.sin(a) * 8), "floor2b_mob", "king_rat");
            if (m != null) lvl.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + 0.3, m.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private void puddles(ServerLevel lvl) {
        play("summon", 22);
        castYaw = getYRot();
        lvl.playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 2f, 0.5f);
        int n = 0;
        for (ServerPlayer p : victims(lvl, 24)) {
            if (n++ >= 5) break;
            Vec3 at = p.position().add(p.getDeltaMovement().scale(6));
            zone(lvl, at, 2.6, 26, GREEN).onHit(v -> { v.hurt(damageSources().mobAttack(this), dmg()); v.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1)); })
                    .fx(c -> {
                        lvl.playSound(null, c.x, c.y, c.z, SoundEvents.BREWING_STAND_BREW, SoundSource.HOSTILE, 1f, 0.6f);
                        for (int t = 0; t < 12; t++) later(t * 10, () -> {   // лужа травит ещё 6 с
                            lvl.sendParticles(ParticleTypes.ITEM_SLIME, c.x, c.y + 0.2, c.z, 10, 1.4, 0.05, 1.4, 0.02);
                            for (ServerPlayer v : victims(lvl, 40))
                                if (Math.hypot(v.getX() - c.x, v.getZ() - c.z) < 2.6 && Math.abs(v.getY() - c.y) < 2) v.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 1));
                        });
                    });
        }
    }

    private void burrow(ServerLevel lvl, ServerPlayer target) {
        play("burrow", 50);
        castYaw = getYRot();
        lvl.playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 2f, 0.6f);
        Vec3 c = home != null ? home : position();
        Vec3 dest = target.position().distanceTo(c) < 13 ? target.position() : c.add((random.nextDouble() - 0.5) * 16, 0, (random.nextDouble() - 0.5) * 16);
        later(20, () -> {
            hidden = 40;
            setInvisible(true);
            teleportTo(dest.x, dest.y, dest.z);
            zone(lvl, dest, 4.0, 22, GREEN).onHit(v -> { v.hurt(damageSources().mobAttack(this), dmg() * 1.5f); knock(v, dest, 0.6, 0.9); })
                    .fx(at -> {
                        hidden = 0;
                        setInvisible(false);
                        triggerAnim("main", "emerge");
                        lvl.playSound(null, blockPosition(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 1.5f, 0.6f);
                        lvl.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.3, at.z, 4, 1.2, 0.1, 1.2, 0);
                        lvl.sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y + 0.3, at.z, 40, 2, 0.4, 2, 0.1);
                    });
        });
    }

    private void sweep(ServerLevel lvl) {
        play("sweep", 22);
        Vec3 c = position();
        lvl.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2f, 0.5f);
        zone(lvl, c, 4.5, 14, RED).onHit(v -> { v.hurt(damageSources().mobAttack(this), dmg() * 1.2f); knock(v, c, 1.4, 0.4); })
                .fx(at -> lvl.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y + 1, at.z, 6, 2, 0.2, 2, 0));
    }

    @Override
    protected void onEnrage(ServerLevel lvl) {
        summon(lvl, 6);
        cooldown = 40;
        for (ServerPlayer p : victims(lvl, 40))
            p.sendSystemMessage(Component.literal("☠ «ПИ-И-И!» Крысиный король напрягает пресс и зовёт всю стаю!").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
