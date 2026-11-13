package com.pvpciv.entity.boss;

import com.pvpciv.combat.Mobs;
import com.pvpciv.entity.FxEntity;
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
import org.joml.Vector3f;

/**
 * Кумихо, девятихвостая лиса (ур. 16) — хозяйка лисьей деревни на западе этажа 3.
 *  • Лисьи огни — синие круги под игроками, взрываются по очереди.
 *  • Мерцание — исчезает и появляется за спиной цели, удар хвостами конусом.
 *  • Очарование — круг вокруг себя: кто внутри в момент удара, теряет скорость и получает урон.
 *  • Свита — зовёт лис-оборотней (не больше 6).
 *  • В ярости (50%) — девять хвостов загораются: огни втрое чаще, мерцает дважды подряд.
 */
public class KumihoBoss extends CivBoss {
    private static final Vector3f FOXFIRE = new Vector3f(0.35f, 0.6f, 1f), PINK = new Vector3f(1f, 0.45f, 0.75f);

    public KumihoBoss(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Override public String defId() { return "kumiho"; }
    @Override public String model() { return "kumiho"; }
    @Override protected String[] actions() { return new String[]{"attack", "cast", "blink", "spin"}; }

    @Override
    protected void ability(ServerLevel lvl, ServerPlayer target) {
        int r = random.nextInt(10);
        if (r < 4) foxfire(lvl);
        else if (r < 7) blink(lvl, target, enraged);
        else if (r < 9) charm(lvl);
        else court(lvl);
        cooldown = enraged ? 45 + random.nextInt(20) : 70 + random.nextInt(30);
    }

    private void foxfire(ServerLevel lvl) {
        play("cast", 26);
        castYaw = getYRot();
        lvl.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.5f, 1.4f);
        int k = 0;
        for (ServerPlayer p : victims(lvl, 24)) for (int i = 0; i < (enraged ? 3 : 1); i++) {
            Vec3 at = p.position().add((random.nextDouble() - 0.5) * 4 * i, 0, (random.nextDouble() - 0.5) * 4 * i);
            later(k++ * 6, () -> zone(lvl, at, 2.2, 22, FOXFIRE).onHit(v -> { v.hurt(damageSources().magic(), dmg() * 1.1f); v.igniteForSeconds(2); })
                    .fx(c -> lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 0.5, c.z, 30, 0.9, 0.5, 0.9, 0.04)));
        }
    }

    private void blink(ServerLevel lvl, ServerPlayer t, boolean twice) {
        Vec3 behind = t.position().subtract(t.getLookAngle().multiply(1, 0, 1).normalize().scale(2.5));
        Vec3 to = safeSpot(lvl, behind, home != null ? home : position());
        FxEntity.spawn(lvl, FxEntity.PILLAR, position(), 0.7f, 14, 0xFF70C0, 0xFFFFFF);
        teleportTo(to.x, to.y, to.z);
        FxEntity.spawn(lvl, FxEntity.PILLAR, to, 0.7f, 14, 0xFF70C0, 0xFFFFFF);
        lvl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2f, 1.4f);
        faceTo(t.position());
        play("spin", 22);
        zone(lvl, to, 5.0, 14, PINK).cone(Math.atan2(t.getZ() - to.z, t.getX() - to.x), Math.toRadians(60))
                .onHit(v -> { v.hurt(damageSources().mobAttack(this), dmg() * 1.3f); knock(v, to, 1.0, 0.4); });
        if (twice) later(26, () -> { if (isAlive() && t.isAlive()) blink(lvl, t, false); });
    }

    private void charm(ServerLevel lvl) {
        play("cast", 30);
        castYaw = getYRot();
        Vec3 c = position();
        lvl.playSound(null, blockPosition(), SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.HOSTILE, 2f, 0.6f);
        zone(lvl, c, 6.5, 30, PINK).onHit(v -> { v.hurt(damageSources().magic(), dmg()); v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3)); v.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0)); });
    }

    private void court(ServerLevel lvl) {
        int near = lvl.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(blockPosition()).inflate(24), m -> m.getTags().contains("kumiho_court")).size();
        if (near >= 6) { foxfire(lvl); return; }
        play("cast", 24);
        castYaw = getYRot();
        Vec3 c = home != null ? home : position();
        for (int i = 0; i < Math.min(3, 6 - near); i++) {
            double a = random.nextDouble() * Math.PI * 2;
            Vec3 at = safeSpot(lvl, c.add(Math.cos(a) * 7, 0, Math.sin(a) * 7), c);
            var m = Mobs.spawn(lvl, Mobs.Kind.FOX, 15, false, at, "floor3b_mob", "kumiho_court");
            if (m != null) FxEntity.spawn(lvl, FxEntity.PILLAR, at, 0.5f, 14, 0xFF9A40, 0xFFE0C0);
        }
    }

    @Override
    protected void onEnrage(ServerLevel lvl) {
        cooldown = 20;
        FxEntity.spawn(lvl, FxEntity.PILLAR, position(), 1.6f, 40, 0x5A9AFF, 0xFF70C0);
        for (ServerPlayer p : victims(lvl, 40))
            p.sendSystemMessage(Component.literal("☠ Девять хвостов Кумихо вспыхивают лисьим огнём!").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
