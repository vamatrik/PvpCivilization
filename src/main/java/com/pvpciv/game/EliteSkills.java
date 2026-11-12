package com.pvpciv.game;

import com.pvpciv.combat.Mobs;
import com.pvpciv.world.CivWorld;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;

/**
 * Удары элиток с подсветкой (бета 12.11): раз в 8–11 с элитка помечает землю под игроком кругом —
 * контур и заполнение растут ~1.2 с, потом удар по всем внутри. Успел выйти из круга — не попал.
 * Ближний бой — красный круг «удар об землю»; стрелки — жёлтый «залп».
 */
public final class EliteSkills {
    private record Tele(Mob mob, ServerLevel lvl, Vec3 c, double r, long start, int dur, boolean ranged) {}
    private static final List<Tele> ACTIVE = new ArrayList<>();
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    private static boolean elite(Mob m) {
        for (String t : m.getTags()) if (t.startsWith("civelite_")) return true;
        return false;
    }

    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        if (now % 5 == 0) for (ServerLevel lvl : new ServerLevel[]{CivWorld.level(server), CivWorld.f2(server)}) {
            if (lvl == null) continue;
            for (ServerPlayer p : lvl.players()) {
                for (Mob m : lvl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(10), m -> m.isAlive() && elite(m) && m.getTarget() == p)) {
                    long at = NEXT.getOrDefault(m.getUUID(), now + 60 + m.getRandom().nextInt(80));
                    NEXT.putIfAbsent(m.getUUID(), at);
                    if (now < at || ACTIVE.stream().anyMatch(t -> t.mob() == m)) continue;
                    NEXT.put(m.getUUID(), now + 160 + m.getRandom().nextInt(60));
                    boolean ranged = m instanceof net.minecraft.world.entity.monster.AbstractSkeleton;
                    if (!ranged && m.distanceTo(p) > 6) continue;
                    ACTIVE.add(new Tele(m, lvl, p.position(), ranged ? 3.2 : 2.8, now, 24, ranged));
                    m.getNavigation().stop();
                    m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 26, 6, false, false));
                    lvl.playSound(null, m.blockPosition(), ranged ? SoundEvents.CROSSBOW_LOADING_MIDDLE.value() : SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.8f, ranged ? 1.2f : 1.6f);
                }
            }
        }
        if (now % 600 == 0) NEXT.keySet().removeIf(id -> ACTIVE.stream().noneMatch(t -> t.mob().getUUID().equals(id)) && NEXT.get(id) < now - 1200);
        Iterator<Tele> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Tele t = it.next();
            if (!t.mob().isAlive()) { it.remove(); continue; }
            float k = Math.min(1f, (now - t.start()) / (float) t.dur());
            Vector3f col = t.ranged() ? new Vector3f(1f, 0.8f, 0.2f) : new Vector3f(1f, 0.15f, 0.12f);
            if (now % 2 == 0) {
                // контур
                for (int i = 0; i < 28; i++) {
                    double a = Math.PI * 2 * i / 28;
                    t.lvl().sendParticles(new DustParticleOptions(col, 1.1f), t.c().x + Math.cos(a) * t.r(), t.c().y + 0.15, t.c().z + Math.sin(a) * t.r(), 1, 0, 0, 0, 0);
                }
                // заполнение — растущий круг (градиент: ярче к краю заполнения)
                double rr = t.r() * k;
                for (int i = 0; i < 18; i++) {
                    double a = Math.PI * 2 * i / 18 + k * 2;
                    t.lvl().sendParticles(new DustParticleOptions(new Vector3f(col).mul(0.6f + 0.4f * k), 0.8f), t.c().x + Math.cos(a) * rr, t.c().y + 0.12, t.c().z + Math.sin(a) * rr, 1, 0, 0, 0, 0);
                }
            }
            if (k < 1f) continue;
            it.remove();
            double dmg = t.mob().getAttributeValue(Attributes.ATTACK_DAMAGE) * (t.ranged() ? 1.3 : 1.6);
            t.lvl().sendParticles(t.ranged() ? ParticleTypes.CRIT : ParticleTypes.EXPLOSION, t.c().x, t.c().y + 0.3, t.c().z, t.ranged() ? 40 : 3, t.r() * 0.5, 0.2, t.r() * 0.5, 0.1);
            t.lvl().playSound(null, net.minecraft.core.BlockPos.containing(t.c()), t.ranged() ? SoundEvents.ARROW_HIT : SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.9f, 1.2f);
            for (ServerPlayer v : t.lvl().getEntitiesOfClass(ServerPlayer.class, new AABB(t.c(), t.c()).inflate(t.r(), 2, t.r()))) {
                if (Math.hypot(v.getX() - t.c().x, v.getZ() - t.c().z) > t.r() || v.isCreative()) continue;
                v.hurt(t.mob().damageSources().mobAttack(t.mob()), (float) dmg);
                if (!t.ranged()) { v.setDeltaMovement(v.getDeltaMovement().add(0, 0.5, 0)); v.hurtMarked = true; }
            }
        }
    }

    @SuppressWarnings("unused") private static final Class<?> M = Mobs.class;
}
