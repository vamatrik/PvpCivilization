package com.pvpciv.game;

import com.pvpciv.combat.Mobs;
import com.pvpciv.entity.boss.CivBoss;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

/**
 * Поводок как в RPG (бета 0.8.7): моб, ушедший далеко от своей точки, сбрасывает агр
 * и сам идёт домой пешком, по дороге восстанавливаясь. Снова агрится, только если по нему ударить.
 * Застрявший в блоках — выталкивается на свободное место.
 */
public final class Leash {
    private Leash() {}
    private static final String HX = "pvpciv_hx", HY = "pvpciv_hy", HZ = "pvpciv_hz", RET = "pvpciv_ret", RT = "pvpciv_rett", WALL = "pvpciv_wall";

    private static boolean eligible(Mob m) {
        return Mobs.level(m) > 0 && !(m instanceof CivBoss) && !ArenaManager.isArena(m);
    }

    private static double radius(Mob m) { return Mobs.lootKind(m).endsWith("_elite") ? 16 : 34; }

    private static boolean provoked(Mob m) {
        LivingEntity by = m.getLastHurtByMob();
        return by instanceof Player && by.isAlive() && m.tickCount - m.getLastHurtByMobTimestamp() < 20 * 5;
    }

    public static void tick(Mob m) {
        // бета 0.9.6: «мобы лежат на полу» — лечение умирающего моба воскрешало его в позе смерти
        if (!(m.level() instanceof ServerLevel lvl) || !m.isAlive() || m.isDeadOrDying() || !eligible(m) || (m.tickCount + m.getId()) % 10 != 0) return;
        CompoundTag t = m.getPersistentData();
        // застрял в камне / под кустом — вытолкнуть
        if (m.isInWall()) {
            int w = t.getInt(WALL) + 10;
            t.putInt(WALL, w);
            if (w >= 20) {
                Vec3 s = Taxi.safe(lvl, m.blockPosition().above());
                m.teleportTo(s.x, s.y, s.z);
                t.putInt(WALL, 0);
            }
        } else t.putInt(WALL, 0);
        if (!t.contains(HX)) { t.putDouble(HX, m.getX()); t.putDouble(HY, m.getY()); t.putDouble(HZ, m.getZ()); return; }
        Vec3 home = new Vec3(t.getDouble(HX), t.getDouble(HY), t.getDouble(HZ));
        double dist = m.position().distanceTo(home), r = radius(m);
        boolean ret = t.getBoolean(RET);
        if (!ret) {
            if (dist > r) { t.putBoolean(RET, true); t.putInt(RT, 0); m.setTarget(null); m.setLastHurtByMob(null); }
            return;
        }
        if (dist < 2.5) {
            t.putBoolean(RET, false);
            m.setHealth(m.getMaxHealth());
            m.getNavigation().stop();
            return;
        }
        if (provoked(m) && dist < r + 24) return;   // ударили по дороге — может огрызнуться
        if (m.getTarget() != null) m.setTarget(null);
        if (m.getHealth() < m.getMaxHealth()) m.heal(m.getMaxHealth() * 0.04f);
        int rt = t.getInt(RT) + 10;
        t.putInt(RT, rt);
        if (rt > 20 * 25 || dist > r + 60) {   // не может дойти — тогда уже перенос
            Vec3 s = Taxi.safe(lvl, BlockPos.containing(home));
            lvl.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + 0.6, m.getZ(), 10, 0.3, 0.4, 0.3, 0.02);
            m.teleportTo(s.x, s.y, s.z);
            t.putBoolean(RET, false);
            m.setHealth(m.getMaxHealth());
            return;
        }
        m.getNavigation().moveTo(home.x, home.y, home.z, 1.25);
    }

    /** Пока моб идёт домой, сам он никого не выбирает целью — только того, кто по нему ударил. */
    public static void onTarget(LivingChangeTargetEvent e) {
        if (!(e.getEntity() instanceof Mob m) || m.level().isClientSide || e.getNewAboutToBeSetTarget() == null) return;
        if (!m.getPersistentData().getBoolean(RET)) return;
        if (provoked(m) && e.getNewAboutToBeSetTarget() == m.getLastHurtByMob()) return;
        e.setCanceled(true);
    }
}
