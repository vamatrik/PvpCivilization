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
    /** fx: 0 — удар/залп (по умолчанию), 1 — споры (яд), 2 — мороз (замедление), 3 — таран (отброс), 4 — кислота (яд+урон), 5 — лисий огонь (горение), 6 — разрез (длинная линия). */
    private record Tele(Mob mob, ServerLevel lvl, Vec3 c, double r, long start, int dur, boolean ranged, int fx) {
        Tele(Mob mob, ServerLevel lvl, Vec3 c, double r, long start, int dur, boolean ranged) { this(mob, lvl, c, r, start, dur, ranged, 0); }
    }
    private static final List<Tele> ACTIVE = new ArrayList<>();
    private static final Map<UUID, Long> NEXT = new HashMap<>();

    private static boolean elite(Mob m) {
        for (String t : m.getTags()) if (t.startsWith("civelite_")) return true;
        return false;
    }

    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        if (now % 5 == 0) for (ServerLevel lvl : new ServerLevel[]{CivWorld.level(server), CivWorld.f2(server), CivWorld.f3(server), CivWorld.f4(server), CivWorld.f5(server)}) {
            if (lvl == null) continue;
            for (ServerPlayer p : lvl.players()) {
                for (Mob m : lvl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(10), m -> m.isAlive() && elite(m) && m.getTarget() == p)) {
                    long at = NEXT.getOrDefault(m.getUUID(), now + 60 + m.getRandom().nextInt(80));
                    NEXT.putIfAbsent(m.getUUID(), at);
                    if (now < at || ACTIVE.stream().anyMatch(t -> t.mob() == m)) continue;
                    NEXT.put(m.getUUID(), now + 160 + m.getRandom().nextInt(60));
                    boolean ranged = m instanceof net.minecraft.world.entity.monster.AbstractSkeleton;
                    // 0.11.2: элитки этажей 3–4 — своя способность (чаще всего), иногда — общий удар
                    if (m.getRandom().nextFloat() < 0.7f && special(m, lvl, p, now)) continue;
                    if (!ranged && m.distanceTo(p) > 6) continue;
                    ACTIVE.add(new Tele(m, lvl, p.position(), ranged ? 3.2 : 2.8, now, 24, ranged));
                    com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.ZONE, p.position(), ranged ? 3.2f : 2.8f, 24, ranged ? 0xFFCC33 : 0xFF2A1E, ranged ? 0xFFF4C0 : 0xFFB0A0);
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
            if (k < 1f) continue;
            it.remove();
            if (t.fx() > 0) { hit(t); continue; }
            double dmg = t.mob().getAttributeValue(Attributes.ATTACK_DAMAGE) * (t.ranged() ? 1.3 : 1.6);
            com.pvpciv.entity.FxEntity.spawn(t.lvl(), com.pvpciv.entity.FxEntity.BURST, t.c(), (float) t.r(), 12, t.ranged() ? 0xFFCC33 : 0xFF2A1E, t.ranged() ? 0xFFF4C0 : 0xFFB0A0);
            t.lvl().playSound(null, net.minecraft.core.BlockPos.containing(t.c()), t.ranged() ? SoundEvents.ARROW_HIT : SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.9f, 1.2f);
            for (ServerPlayer v : t.lvl().getEntitiesOfClass(ServerPlayer.class, new AABB(t.c(), t.c()).inflate(t.r(), 2, t.r()))) {
                if (Math.hypot(v.getX() - t.c().x, v.getZ() - t.c().z) > t.r() || v.isCreative()) continue;
                v.hurt(t.mob().damageSources().mobAttack(t.mob()), (float) dmg);
                if (!t.ranged()) { v.setDeltaMovement(v.getDeltaMovement().add(0, 0.5, 0)); v.hurtMarked = true; }
            }
        }
    }

    // ------------------------------------------------------------------ 0.11.2: уникальные способности элиток
    private static final int[][] COL = {{0xFF2A1E, 0xFFB0A0}, {0x6AD040, 0xD8FFB0}, {0x60C8FF, 0xE0F8FF}, {0xFF8A20, 0xFFE0B0}, {0xA8F040, 0xF0FFC0}, {0xFF6A20, 0xFFD080}, {0xFF3040, 0xFFD0D8}};

    private static void zone(Mob m, ServerLevel lvl, Vec3 c, double r, long now, int dur, int fx) {
        ACTIVE.add(new Tele(m, lvl, c, r, now, dur, false, fx));
        com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.ZONE, c, (float) r, dur, COL[fx][0], COL[fx][1]);
    }

    /** true — способность применена. */
    private static boolean special(Mob m, ServerLevel lvl, ServerPlayer p, long now) {
        Mobs.Kind k = Mobs.Kind.of(m.getType());
        if (k == null) return false;
        Vec3 pp = p.position();
        switch (k) {
            case BOTANIST, SPORELING -> {   // облако спор: большой круг под игроком
                zone(m, lvl, pp, 4.2, now, 30, 1);
                lvl.playSound(null, m.blockPosition(), SoundEvents.SNIFFER_SNIFFING, SoundSource.HOSTILE, 1.2f, 0.7f);
            }
            case THAWED, VOID_CRAWLER -> {   // ледяная (пустотная) волна вокруг себя
                if (m.distanceTo(p) > 7) return false;
                zone(m, lvl, m.position(), 6.0, now, 30, 2);
                lvl.playSound(null, m.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5f, 0.5f);
            }
            case ROBOT, ROGUE_ASTRO -> {   // таран: три круга по прямой к игроку
                Vec3 dir = pp.subtract(m.position()).multiply(1, 0, 1).normalize();
                for (int i = 0; i < 3; i++) zone(m, lvl, m.position().add(dir.scale(2.5 + i * 2.6)), 1.9, now, 26 + i * 3, 3);
                lvl.playSound(null, m.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.HOSTILE, 1.5f, 0.6f);
            }
            case XENO, CHIMERA -> {   // плевок кислотой: три лужи вокруг игрока
                for (int i = 0; i < 3; i++) {
                    double a = Math.PI * 2 * i / 3 + m.getRandom().nextDouble();
                    zone(m, lvl, pp.add(Math.cos(a) * 2.2, 0, Math.sin(a) * 2.2), 2.0, now, 28, 4);
                }
                lvl.playSound(null, m.blockPosition(), SoundEvents.LLAMA_SPIT, SoundSource.HOSTILE, 1.5f, 0.6f);
            }
            case KITSUNE_ARCHER, FOX -> {   // лисий огонь: круг, потом ещё один там, куда отбежал
                zone(m, lvl, pp, 2.6, now, 24, 5);
                lvl.playSound(null, m.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1f, 1.3f);
            }
            case SAMURAI, WEREWOLF, COLLECTOR -> {   // иай-разрез (коллектор — «росчерк пера»): длинная полоса через игрока
                Vec3 dir = pp.subtract(m.position()).multiply(1, 0, 1).normalize();
                for (int i = 0; i < 5; i++) zone(m, lvl, m.position().add(dir.scale(1.5 + i * 1.6)), 1.3, now, 22, 6);
                lvl.playSound(null, m.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5f, 0.6f);
            }
            case NINJA -> {   // рывок за спину
                Vec3 behind = pp.subtract(p.getLookAngle().multiply(1, 0, 1).normalize().scale(2));
                lvl.sendParticles(ParticleTypes.LARGE_SMOKE, m.getX(), m.getY() + 1, m.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
                m.teleportTo(behind.x, pp.y, behind.z);
                lvl.playSound(null, m.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 1.5f);
                zone(m, lvl, pp, 2.2, now, 16, 6);
            }
            default -> { return false; }
        }
        NEXT.put(m.getUUID(), now + 150 + m.getRandom().nextInt(60));
        m.getNavigation().stop();
        m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 4, false, false));
        return true;
    }

    private static void hit(Tele t) {
        double base = t.mob().getAttributeValue(Attributes.ATTACK_DAMAGE);
        com.pvpciv.entity.FxEntity.spawn(t.lvl(), com.pvpciv.entity.FxEntity.BURST, t.c(), (float) t.r(), 12, COL[t.fx()][0], COL[t.fx()][1]);
        switch (t.fx()) {
            case 1 -> t.lvl().sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, t.c().x, t.c().y + 0.5, t.c().z, 40, t.r() / 2, 0.4, t.r() / 2, 0.01);
            case 2 -> t.lvl().sendParticles(ParticleTypes.SNOWFLAKE, t.c().x, t.c().y + 0.5, t.c().z, 50, t.r() / 2, 0.4, t.r() / 2, 0.05);
            case 5 -> t.lvl().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, t.c().x, t.c().y + 0.3, t.c().z, 30, t.r() / 2, 0.3, t.r() / 2, 0.02);
            default -> {}
        }
        for (ServerPlayer v : t.lvl().getEntitiesOfClass(ServerPlayer.class, new AABB(t.c(), t.c()).inflate(t.r(), 2, t.r()))) {
            if (Math.hypot(v.getX() - t.c().x, v.getZ() - t.c().z) > t.r() || v.isCreative()) continue;
            double mul = switch (t.fx()) { case 1 -> 0.9; case 2 -> 1.2; case 3 -> 1.5; case 4 -> 1.0; case 5 -> 1.1; default -> 1.3; };
            v.hurt(t.mob().damageSources().mobAttack(t.mob()), (float) (base * mul));
            switch (t.fx()) {
                case 1 -> v.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
                case 2 -> v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 3));
                case 3 -> { Vec3 d = v.position().subtract(t.mob().position()).multiply(1, 0, 1).normalize().scale(1.4); v.setDeltaMovement(d.x, 0.5, d.z); v.hurtMarked = true; }
                case 4 -> v.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 2));
                case 5 -> v.igniteForSeconds(3);
                default -> {}
            }
        }
    }

    @SuppressWarnings("unused") private static final Class<?> M = Mobs.class;
}
