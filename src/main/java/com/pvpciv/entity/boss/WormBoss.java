package com.pvpciv.entity.boss;

import net.minecraft.core.BlockPos;
import com.pvpciv.floor.Belt;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.*;

/**
 * 1.0 — Галактический червь, фаза 1 финала (пояс астероидов). Летает кругами над полем астероидов:
 *  • глотает астероиды (на камне предупреждение — прыгай!), камни возвращаются через 25 с;
 *  • сферы пустоты — круги под игроками;
 *  • таранный рывок: по земле линия-предупреждение, рывок убивает с одного удара (если ты не перерос его на 3+ ур. — тогда 45% здоровья).
 *    После рывка червь лежит на центральном астероиде: бей! Встаёт, потеряв 1/3 здоровья, или через 10 с — и бьёт волной.
 * В полёте получает 60% урона, лёжа — 150%. Тело — сегменты; удар по сегменту ранит голову.
 * Смерть → взрыв, тьма, фаза 2 (демоница Пустоты, {@link com.pvpciv.game.VoidRift}).
 */
public class WormBoss extends CivBoss {
    private static final Vector3f VOID = new Vector3f(0.55f, 0.2f, 0.9f), GLOW = new Vector3f(1f, 0.35f, 0.9f), RED = new Vector3f(1f, 0.15f, 0.2f);
    public static final int SEGMENTS = 14;
    private static final double SPACING = 2.4;

    private enum St { INTRO, ORBIT, SWALLOW, DASH_AIM, DASH, STUNNED }
    private St st = St.INTRO;
    private int t, actions;
    private double orbitA;
    private Vec3 vel = Vec3.ZERO, goal, dashFrom, dashTo;
    private Belt.Rock rockTarget;
    private float stunHp;
    private final ArrayDeque<Vec3> trail = new ArrayDeque<>();
    private final List<UUID> segs = new ArrayList<>();
    private final Set<UUID> dashHit = new HashSet<>();
    /** 1.1.1: чёрные дыры в полёте — тянут к себе (можно сорваться с камня). */
    private static final class Hole { final Vec3 c; int t; Hole(Vec3 c, int t) { this.c = c; this.t = t; } }
    private final List<Hole> holes = new ArrayList<>();

    public WormBoss(EntityType<? extends Monster> type, Level level) { super(type, level); setNoGravity(true); }

    @Override public String defId() { return "worm"; }
    @Override public String model() { return "worm_head"; }
    @Override protected String[] actions() { return new String[]{"attack", "roar", "stunned"}; }
    @Override protected void registerGoals() { targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, Player.class, false)); }
    @Override protected void ability(ServerLevel lvl, ServerPlayer target) {}
    @Override public boolean isPushable() { return false; }

    private Vec3 c() { return home != null ? home : Vec3.atBottomCenterOf(Belt.CENTER); }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.getEntity() instanceof Player) amount *= st == St.STUNNED ? 1.5f : 0.6f;
        boolean r = super.hurt(src, amount);
        if (r && st == St.STUNNED && stunHp - getHealth() >= getMaxHealth() / 3f) getUp();
        return r;
    }

    // ------------------------------------------------------------------ ход
    @Override
    protected void passive(ServerLevel lvl) {
        noPhysics = true;
        cooldown = 9999;
        setDeltaMovement(Vec3.ZERO);
        t++;
        if (segs.isEmpty() && tickCount > 2) spawnSegments(lvl);
        switch (st) {
            case INTRO -> intro(lvl);
            case ORBIT -> orbit(lvl);
            case SWALLOW -> swallowTick(lvl);
            case DASH_AIM -> { hover(dashFrom, 0.9); if (t % 12 == 0 && t < 30) for (ServerPlayer p : pick(victims(lvl, 60), 5)) fan(lvl, mouth(), p, 3, 12, bolt(), dmg() * 0.3f, 1.1f); if (t >= 32) { /* 1.2: задержка 50 → 32 */ st = St.DASH; t = 0; dashHit.clear(); lvl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3f, 0.7f); } }
            case DASH -> dashTick(lvl);
            case STUNNED -> {
                if (t % 10 == 0) lvl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 2, getZ(), 10, 1, 0.5, 1, 0.1);
                if (t >= 200) getUp();
            }
        }
        holesTick(lvl);
        raysTick(lvl);
        // хвост
        trail.addFirst(position());
        while (trail.size() > 400) trail.removeLast();
        placeSegments(lvl);
    }

    private void intro(ServerLevel lvl) {
        if (t == 1) {
            triggerAnim("main", "roar");
            for (ServerPlayer p : victims(lvl, 80)) {
                PacketDistributor.sendToPlayer(p, new Network.PhoneData(22, "{\"cut\":\"worm\",\"len\":90}"));
                p.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2f, 0.5f);
            }
        }
        hover(c().add(0, 14, 0), 0.25);
        if (t >= 90) { st = St.ORBIT; t = 0; }
    }

    private void hover(Vec3 to, double speed) {
        Vec3 d = to.subtract(position());
        Vec3 want = d.length() < 0.5 ? Vec3.ZERO : d.normalize().scale(Math.min(speed, d.length()));
        vel = vel.lerp(want, 0.15);
        step();
    }

    private void step() {
        Vec3 n = position().add(vel);
        if (n.y < c().y - 4) n = new Vec3(n.x, c().y - 4, n.z);
        setPos(n.x, n.y, n.z);
        if (vel.lengthSqr() > 1e-3) {
            float yaw = (float) (Mth.atan2(vel.z, vel.x) * Mth.RAD_TO_DEG) - 90f;
            float pitch = (float) -(Mth.atan2(vel.y, Math.hypot(vel.x, vel.z)) * Mth.RAD_TO_DEG);
            setYRot(yaw); yBodyRot = yaw; yHeadRot = yaw; setXRot(pitch);
        }
    }

    /**
     * 1.1.2: в полёте червь не даёт передышки — по кругу меняет «узор» стрельбы (по каждому игроку):
     *  поток — сгусток каждые 6 тиков с упреждением; веер — 5 сгустков; кольцо — 14 во все стороны;
     *  лучи — тонкий прицел 0.9 с на каждого, затем залп. Раз в 3.5–5 с — большое действие (таран, глоток, обстрел, сферы).
     */
    private int pattern;

    private void orbit(ServerLevel lvl) {
        double speed = enraged ? 0.75 : 0.6;
        orbitA += speed / 24.0;
        Vec3 c = c();
        Vec3 to = c.add(Math.cos(orbitA) * 24, 7 + Math.sin(orbitA * 2.3) * 4, Math.sin(orbitA) * 24);
        hover(to, speed * 1.4);
        List<ServerPlayer> vs = victims(lvl, 60);
        if (vs.isEmpty()) return;
        if (t % 50 == 1) pattern = (pattern + 1 + random.nextInt(2)) % 4;
        firePattern(lvl, vs);
        if (t > (enraged ? 70 : 100)) {
            t = 0;
            actions++;
            ServerPlayer target = vs.get(random.nextInt(vs.size()));
            if (actions % 3 == 0) aimDash(lvl, target);
            else switch (random.nextInt(enraged ? 6 : 5)) {
                case 0 -> startSwallow(lvl, vs);
                case 1 -> bombard(lvl, vs);
                case 2 -> spheres(lvl, vs);
                case 3, 4 -> lasers(lvl, vs, 1);
                default -> holes(lvl);   // чёрные дыры — редко и только в ярости
            }
        }
    }

    private static net.minecraft.world.item.ItemStack BOLT;
    private net.minecraft.world.item.ItemStack bolt() { if (BOLT == null) BOLT = new net.minecraft.world.item.ItemStack(com.pvpciv.registry.ModV11.VOID_BOLT.get()); return BOLT; }
    private Vec3 mouth() { return position().add(getLookAngle().scale(1.5)).add(0, 1.0, 0); }

    /** 1.2: цели стрельбы — не больше 5 игроков за раз (каждую секунду набор тасуется). */
    private List<ServerPlayer> shotTargets = new ArrayList<>();
    private List<ServerPlayer> pick(List<ServerPlayer> vs, int cap) {
        if (vs.size() <= cap) return vs;
        List<ServerPlayer> l = new ArrayList<>(vs);
        java.util.Collections.shuffle(l, new java.util.Random(random.nextLong()));
        return l.subList(0, cap);
    }

    private static net.minecraft.world.item.ItemStack SEEKER;
    private net.minecraft.world.item.ItemStack seeker() { if (SEEKER == null) SEEKER = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FIRE_CHARGE); return SEEKER; }

    /**
     * 1.2 (по отзыву): червь почти без пауз стреляет магическими сгустками по прямой в каждого (до 5 игроков);
     * в ярости часть сгустков — огненные с лёгким самонаведением. Узоры (веер, кольцо) — для разнообразия поверх потока.
     */
    private void firePattern(ServerLevel lvl, List<ServerPlayer> vs) {
        Vec3 m = mouth();
        float d = dmg();
        if (t % 20 == 1 || shotTargets.isEmpty()) shotTargets = pick(vs, 5);
        if (t % (enraged ? 6 : 8) == 0) {
            for (ServerPlayer p : shotTargets) {
                if (!p.isAlive()) continue;
                if (enraged && random.nextFloat() < 0.22f) {
                    BossShot s = bolt(lvl, m, p, seeker(), d * 0.3f, 0.85f, 1f);
                    s.homing = p; s.turn = 0.045; s.trail = ParticleTypes.FLAME;
                } else bolt(lvl, m, p, bolt(), d * 0.26f, 1.2f, 2f);
            }
            lvl.playSound(null, blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.HOSTILE, 1.2f, 0.8f + random.nextFloat() * 0.4f);
        }
        switch (pattern) {
            case 1 -> {   // веер
                if (t % (enraged ? 22 : 30) == 0) {
                    for (ServerPlayer p : shotTargets) fan(lvl, m, p, enraged ? 7 : 5, 9, bolt(), d * 0.3f, 1.0f);
                    lvl.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.6f, 0.6f);
                }
            }
            case 2 -> {   // кольцо (наклонено вниз — к астероидам)
                if (t % (enraged ? 24 : 34) == 0) {
                    ringBurst(lvl, m, enraged ? 18 : 14, 0.25, bolt(), d * 0.28f, 0.9f);
                    lvl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 1.8f, 0.7f);
                }
            }
            default -> {}
        }
    }

    // ---- 1.2: луч копится во рту и ведёт игрока с запаздыванием (до 3 игроков)
    private static final class Ray { final ServerPlayer p; Vec3 aim; int t; com.pvpciv.entity.FxEntity fx; Ray(ServerPlayer p, Vec3 aim) { this.p = p; this.aim = aim; } }
    private final List<Ray> rays = new ArrayList<>();
    private int rayCharge = -1;
    private static final int RAY_CHARGE = 34, RAY_LEN = 60;

    private void lasers(ServerLevel lvl, List<ServerPlayer> vs, int n) {
        if (rayCharge >= 0 || !rays.isEmpty()) return;
        rayCharge = 0;
        triggerAnim("main", "roar");
        if (enraged || random.nextBoolean()) com.pvpciv.game.SkySpin.start(lvl, c(), 90, 70);
        lvl.playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 3f, 0.6f);
        for (ServerPlayer p : vs) p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(Component.literal("⚠ Червь копит луч — не стой на месте!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)));
    }

    private void raysTick(ServerLevel lvl) {
        Vec3 m = mouth();
        if (rayCharge >= 0) {   // заряд: свет стягивается в пасть
            rayCharge++;
            for (int i = 0; i < 6; i++) {
                Vec3 o = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize().scale(3.5);
                lvl.sendParticles(ParticleTypes.END_ROD, m.x + o.x, m.y + o.y, m.z + o.z, 0, -o.x, -o.y, -o.z, 0.08);
            }
            if (rayCharge % 6 == 0) lvl.sendParticles(new net.minecraft.core.particles.DustParticleOptions(GLOW, 2.5f), m.x, m.y, m.z, 6, 0.2, 0.2, 0.2, 0);
            if (rayCharge >= RAY_CHARGE) {
                rayCharge = -1;
                for (ServerPlayer p : pick(victims(lvl, 60), enraged ? 3 : 2)) {
                    Ray r = new Ray(p, p.position().add(0, 1, 0).add(p.getDeltaMovement().scale(-6)));
                    r.fx = com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.BEAM, m, 0.7f, RAY_LEN + 4, light(GLOW), 0xFFFFFF);
                    rays.add(r);
                }
                lvl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.5f, 1.3f);
            }
            return;
        }
        for (var it = rays.iterator(); it.hasNext(); ) {
            Ray r = it.next();
            r.t++;
            Vec3 want = r.p.isAlive() ? r.p.position().add(0, 1, 0) : r.aim;
            r.aim = r.aim.add(want.subtract(r.aim).scale(enraged ? 0.075 : 0.06));   // ведёт с запаздыванием
            Vec3 dir = r.aim.subtract(m);
            Vec3 end = m.add(dir.normalize().scale(Math.max(dir.length() + 12, 30)));
            if (r.fx != null) { r.fx.setPos(m.x, m.y, m.z); r.fx.to(end); }
            if (r.t % 3 == 0) for (int i = 1; i <= 8; i++) { Vec3 q = m.lerp(end, i / 8.0); lvl.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0.15, 0.15, 0.15, 0.01); }
            if (r.t % 6 == 0) for (ServerPlayer p : victims(lvl, 80)) {
                if (distToSegment(p.position().add(0, p.getBbHeight() * 0.5, 0), m, end) < 1.1) {
                    p.invulnerableTime = 0;
                    p.hurt(damageSources().indirectMagic(this, this), dmg() * 0.45f);
                }
            }
            if (r.t >= RAY_LEN) { if (r.fx != null) r.fx.discard(); it.remove(); }
        }
    }

    // ---- глотает астероид
    private void startSwallow(ServerLevel lvl, List<ServerPlayer> vs) {
        Belt.Rock best = null;
        for (Belt.Rock k : Belt.ROCKS) {
            if (Belt.gone(k.id()) || Vec3.atCenterOf(k.c()).distanceTo(c()) > 30) continue;
            boolean occupied = false;
            for (ServerPlayer p : vs) if (onRock(p, k)) occupied = true;
            if (best == null || occupied || random.nextInt(4) == 0) { best = k; if (occupied) break; }
        }
        if (best == null) { spheres(lvl, vs); return; }
        rockTarget = best;
        st = St.SWALLOW;
        t = 0;
        Belt.Rock k = best;
        for (ServerPlayer p : vs) if (onRock(p, k) || p.distanceToSqr(Vec3.atCenterOf(k.c())) < 100)
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(Component.literal("⚠ ЧЕРВЬ ИДЁТ ЗА ТВОИМ АСТЕРОИДОМ — ПРЫГАЙ!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
        com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.ZONE, Vec3.atBottomCenterOf(k.c()), k.r() + 1.2f, 50, com.pvpciv.entity.FxEntity.rgb(RED), light(RED));
        lvl.playSound(null, k.c(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2f, 1.2f);
    }

    private static boolean onRock(ServerPlayer p, Belt.Rock k) {
        return Math.hypot(p.getX() - k.c().getX() - 0.5, p.getZ() - k.c().getZ() - 0.5) < k.r() + 1.3 && Math.abs(p.getY() - k.c().getY()) < 2.5;
    }

    private void swallowTick(ServerLevel lvl) {
        Belt.Rock k = rockTarget;
        if (k == null) { st = St.ORBIT; return; }
        Vec3 at = Vec3.atCenterOf(k.c());
        if (t % 4 == 0) lvl.sendParticles(ParticleTypes.FALLING_OBSIDIAN_TEAR, at.x, at.y + 0.2, at.z, 8, k.r(), 0.1, k.r(), 0);
        if (t < 40) {
            hover(at.add(0, 10, 0).add(at.subtract(c()).normalize().scale(6)), 0.8);
            if (t % 8 == 0) for (ServerPlayer p : victims(lvl, 60)) bolt(lvl, mouth(), p, bolt(), dmg() * 0.28f, 1.2f, 3f);
            return;
        }
        hover(at, 1.3);
        if (position().distanceTo(at) < 2.5 || t > 90) {
            triggerAnim("main", "attack");
            for (ServerPlayer p : victims(lvl, 40)) if (onRock(p, k)) {
                p.invulnerableTime = 0;
                p.hurt(damageSources().mobAttack(this), dmg() * 2.5f);
                knock(p, at, 1.2, 0.9);
            }
            Belt.swallow(lvl, k);
            lvl.playSound(null, k.c(), SoundEvents.GENERIC_EAT, SoundSource.HOSTILE, 3f, 0.4f);
            lvl.playSound(null, k.c(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5f, 0.6f);
            rockTarget = null;
            st = St.ORBIT; t = 0;
        }
    }

    // ---- 1.1.1: обстрел астероида — метка, 3–5 сгустков, камень разлетается
    private void bombard(ServerLevel lvl, List<ServerPlayer> vs) {
        boolean main = !Belt.centerGone() && random.nextInt(enraged ? 3 : 4) == 0;
        Belt.Rock k = null;
        if (!main) {
            List<Belt.Rock> cand = new ArrayList<>();
            for (Belt.Rock r : Belt.ROCKS) if (!Belt.gone(r.id()) && Vec3.atCenterOf(r.c()).distanceTo(c()) < 32) cand.add(r);
            for (Belt.Rock r : cand) for (ServerPlayer p : vs) if (onRock(p, r)) { k = r; break; }
            if (k == null && !cand.isEmpty()) k = cand.get(random.nextInt(cand.size()));
            if (k == null) { spheres(lvl, vs); return; }
        }
        final Belt.Rock rock = k;
        Vec3 at = main ? Vec3.atBottomCenterOf(Belt.CENTER) : Vec3.atBottomCenterOf(rock.c());
        float r = main ? 9.5f : rock.r() + 1.2f;
        int warn = main ? 70 : 45;
        com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.ZONE, at, r, warn + 12, com.pvpciv.entity.FxEntity.rgb(RED), light(RED));
        triggerAnim("main", "roar");
        lvl.playSound(null, BlockPos.containing(at), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.5f, 0.8f);
        for (ServerPlayer p : vs) if (main ? p.distanceToSqr(at) < 14 * 14 : onRock(p, rock) || p.distanceToSqr(at) < 64)
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(Component.literal(main ? "⚠ ЧЕРВЬ ЦЕЛИТСЯ В ЦЕНТР — УХОДИ НА КАМНИ!" : "⚠ ЧЕРВЬ ЦЕЛИТСЯ В ТВОЙ АСТЕРОИД!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
        int n = 3 + random.nextInt(3);
        for (int i = 0; i < n; i++) later(warn - 14 + i * 3, () -> {
            if (!isAlive()) return;
            Vec3 from = position().add(0, 1.2, 0);
            Vec3 tgt = at.add((random.nextDouble() - 0.5) * r, 0.5, (random.nextDouble() - 0.5) * r);
            Vec3 dir = tgt.subtract(from);
            BossShot s = new BossShot(lvl, this, dmg() * 0.5f, new net.minecraft.world.item.ItemStack(com.pvpciv.registry.ModF5.VOID_SHARD.get()));
            s.setPos(from.x, from.y, from.z);
            s.shoot(dir.x, dir.y, dir.z, (float) Math.max(1.2, dir.length() / 12.0), 0f);
            lvl.addFreshEntity(s);
            lvl.playSound(null, blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.HOSTILE, 2f, 0.5f);
        });
        later(warn + 2, () -> {
            if (!isAlive()) return;
            lvl.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5f, 0.6f);
            for (ServerPlayer p : victims(lvl, 60)) if (main ? p.distanceToSqr(at) < r * r && Math.abs(p.getY() - at.y) < 3 : onRock(p, rock)) {
                p.invulnerableTime = 0;
                p.hurt(damageSources().mobAttack(this), dmg() * 1.6f);
                knock(p, at, 0.6, 0.7);
            }
            if (main) Belt.breakCenter(lvl); else Belt.swallow(lvl, rock);
        });
    }

    // ---- 1.1.1: чёрные дыры
    private void holes(ServerLevel lvl) {
        List<ServerPlayer> vs = victims(lvl, 60);
        int n = enraged ? 2 : 1;
        for (int i = 0; i < n && !vs.isEmpty(); i++) {
            ServerPlayer p = vs.get(random.nextInt(vs.size()));
            Vec3 hc = p.position().add((random.nextDouble() - 0.5) * 8, 1.2 + random.nextDouble() * 2, (random.nextDouble() - 0.5) * 8);
            later(10 + i * 8, () -> {
                holes.add(new Hole(hc, 110));
                com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.ZONE, new Vec3(hc.x, hc.y - 1.2, hc.z), 4.5f, 110, com.pvpciv.entity.FxEntity.rgb(VOID), light(GLOW));
                lvl.playSound(null, BlockPos.containing(hc), SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 1.2f, 1.5f);
            });
        }
    }

    private void holesTick(ServerLevel lvl) {
        for (Iterator<Hole> it = holes.iterator(); it.hasNext(); ) {
            Hole h = it.next();
            if (--h.t <= 0) { it.remove(); lvl.sendParticles(ParticleTypes.REVERSE_PORTAL, h.c.x, h.c.y, h.c.z, 60, 0.5, 0.5, 0.5, 0.6); continue; }
            lvl.sendParticles(ParticleTypes.PORTAL, h.c.x, h.c.y, h.c.z, 8, 1.4, 1.0, 1.4, 1.2);
            if (h.t % 4 == 0) lvl.sendParticles(ParticleTypes.SQUID_INK, h.c.x, h.c.y, h.c.z, 2, 0.15, 0.15, 0.15, 0);
            for (ServerPlayer p : victims(lvl, 60)) {
                Vec3 to = h.c.subtract(p.position().add(0, 0.9, 0));
                double d = to.length();
                if (d > 7 || d < 0.1) continue;
                double pull = 0.05 * (1 - d / 7) + 0.02;   // в низкой гравитации тянет заметно — держись за камень
                p.setDeltaMovement(p.getDeltaMovement().add(to.normalize().scale(pull).multiply(1, 0.3, 1)));
                p.hurtMarked = true;
                if (d < 1.4 && h.t % 10 == 0) { p.invulnerableTime = 0; p.hurt(damageSources().magic(), dmg() * 0.6f); }
            }
        }
    }

    // ---- сферы пустоты
    private void spheres(ServerLevel lvl, List<ServerPlayer> vs) {
        triggerAnim("main", "roar");
        int volleys = enraged ? 4 : 3;
        for (int v = 0; v < volleys; v++) later(v * 18, () -> {
            if (!isAlive()) return;
            for (ServerPlayer p : victims(lvl, 60)) {
                Vec3 at = p.position();
                Vec3 from = position();
                for (int i = 0; i < 10; i++) { Vec3 q = from.lerp(at, i / 10.0); lvl.sendParticles(ParticleTypes.DRAGON_BREATH, q.x, q.y + 1, q.z, 2, 0.1, 0.1, 0.1, 0); }
                zone(lvl, at, 2.6, 28, VOID).onHit(pp -> pp.hurt(damageSources().indirectMagic(this, this), dmg() * 1.5f))
                        .fx(c2 -> lvl.sendParticles(ParticleTypes.REVERSE_PORTAL, c2.x, c2.y + 0.5, c2.z, 40, 1, 1, 1, 0.3));
            }
            lvl.playSound(null, blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.HOSTILE, 2f, 0.5f);
        });
    }

    // ---- таранный рывок
    private void aimDash(ServerLevel lvl, ServerPlayer target) {
        Vec3 c = c();
        Vec3 tp = target.position();
        Vec3 dir = tp.subtract(c).multiply(1, 0, 1);
        dir = dir.lengthSqr() < 1 ? new Vec3(1, 0, 0) : dir.normalize();
        dashFrom = c.subtract(dir.scale(26)).add(0, 1.2, 0);   // 1.2: таран всегда в главный остров (раньше — на высоте игрока, бывало мимо)
        dashTo = c.add(dir.scale(8)).add(0, 0.5, 0);
        st = St.DASH_AIM;
        t = 0;
        if (enraged) com.pvpciv.game.SkySpin.start(lvl, c, 90, 50);
        // линия-предупреждение по земле: от края через цель к центру
        for (int i = 0; i <= 17; i++) {
            Vec3 at = dashFrom.lerp(dashTo, i / 17.0);
            final Vec3 a = new Vec3(at.x, c.y, at.z);
            com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.ZONE, a, 2.0f, 34, com.pvpciv.entity.FxEntity.rgb(RED), light(RED));
        }
        for (ServerPlayer p : victims(lvl, 60)) {
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(Component.literal("⚠ ТАРАН! Уходи с красной линии!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
        }
        lvl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3f, 0.6f);
    }

    private void dashTick(ServerLevel lvl) {
        Vec3 d = dashTo.subtract(position());
        vel = d.length() < 1.6 ? d : d.normalize().scale(1.6);
        step();
        if (t % 2 == 0) lvl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
        int lvlM = com.pvpciv.combat.Mobs.level(this);
        for (ServerPlayer p : victims(lvl, 6)) {
            // 1.5.2: зона удара = красная линия (2.0) + полтела игрока; было 3.2 — убивало рядом с линией
            if (Math.hypot(p.getX() - getX(), p.getZ() - getZ()) > 2.3 || Math.abs(p.getY() - getY()) > 3 || !dashHit.add(p.getUUID())) continue;
            boolean over = Progress.d(p).level >= lvlM + 3;
            p.invulnerableTime = 0;
            p.hurt(damageSources().mobAttack(this), p.getMaxHealth() * (over ? 0.45f : 5f));
            knock(p, position(), 1.5, 0.8);
        }
        if (d.length() < 1.7 || t > 80) {
            Belt.restoreCenter(lvl);   // 1.1.1: лёг на разбитый центр — центр собирается под ним
            st = St.STUNNED; t = 0;
            stunHp = getHealth();
            Vec3 s = c().add(0, 0, 0);
            setPos(s.x, s.y, s.z);
            setXRot(0);
            triggerAnim("main", "stunned");
            lvl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 3f, 0.4f);
            lvl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
            for (ServerPlayer p : victims(lvl, 60)) p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket(Component.literal("Червь оглушён — БЕЙ! (×1.5 урона)").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        }
    }

    private void getUp() {
        if (!(level() instanceof ServerLevel lvl)) return;
        st = St.ORBIT; t = 0;
        triggerAnim("main", "roar");
        wave(lvl, position(), 14, 0.5, dmg() * 1.3f, GLOW);
        lvl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3f, 0.9f);
        vel = new Vec3(0, 0.6, 0);
    }

    // ------------------------------------------------------------------ тело
    /** Сегменты, которые голова ведёт сейчас (остальные с её UUID — лишние). */
    public boolean owns(UUID seg) { return segs.contains(seg); }

    private void spawnSegments(ServerLevel lvl) {
        // 1.5.2: после выгрузки/загрузки чанка список сегментов пуст, а старое тело осталось в мире —
        // раньше голова спавнила новое тело, и старое лежало «вторым червём». Теперь подбираем своё.
        List<WormSegment> mine = lvl.getEntitiesOfClass(WormSegment.class, getBoundingBox().inflate(120), w -> getUUID().equals(w.head));
        mine.sort(Comparator.comparingInt(w -> w.index));
        boolean full = mine.size() == SEGMENTS;
        for (int i = 0; full && i < SEGMENTS; i++) if (mine.get(i).index != i) full = false;
        if (full) { for (WormSegment w : mine) segs.add(w.getUUID()); return; }
        for (WormSegment w : mine) w.discard();
        for (int i = 0; i < SEGMENTS; i++) {
            WormSegment s = com.pvpciv.registry.ModMobs.WORM_SEG.get().create(lvl);
            if (s == null) return;
            s.head = getUUID();
            s.index = i;
            s.moveTo(getX(), getY(), getZ(), 0, 0);
            lvl.addFreshEntity(s);
            segs.add(s.getUUID());
        }
    }

    private void placeSegments(ServerLevel lvl) {
        if (segs.isEmpty()) return;
        Iterator<Vec3> it = trail.iterator();
        Vec3 prev = it.hasNext() ? it.next() : position();
        double acc = 0;
        int i = 0;
        Vec3 last = prev;
        while (i < segs.size()) {
            double want = SPACING * (i + 1);
            Vec3 cur = it.hasNext() ? it.next() : null;
            if (cur == null) { for (; i < segs.size(); i++) place(lvl, segs.get(i), last, last); break; }
            double d = cur.distanceTo(prev);
            if (acc + d >= want) {
                double k = d < 1e-6 ? 0 : (want - acc) / d;
                Vec3 at = prev.lerp(cur, k);
                place(lvl, segs.get(i), at, last);
                last = at;
                i++;
                continue;   // тот же отрезок может дать ещё сегмент
            }
            acc += d;
            prev = cur;
        }
    }

    private void place(ServerLevel lvl, UUID id, Vec3 at, Vec3 toward) {
        if (!(lvl.getEntity(id) instanceof WormSegment s)) return;
        s.setPos(at.x, at.y, at.z);
        Vec3 v = toward.subtract(at);
        if (v.lengthSqr() > 1e-4) {
            float yaw = (float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90f;
            s.setYRot(yaw); s.yBodyRot = yaw; s.yHeadRot = yaw;
            s.setXRot((float) -(Mth.atan2(v.y, Math.hypot(v.x, v.z)) * Mth.RAD_TO_DEG));
        }
    }

    @Override
    protected void onEnrage(ServerLevel lvl) {
        com.pvpciv.game.SkySpin.start(lvl, c(), 90, 90);
        for (ServerPlayer p : victims(lvl, 70)) p.sendSystemMessage(Component.literal("☄ Галактический червь в ярости: быстрее и злее!").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public void die(DamageSource src) {
        if (level() instanceof ServerLevel lvl) {
            for (UUID u : segs) {
                if (lvl.getEntity(u) instanceof WormSegment s) {
                    lvl.sendParticles(ParticleTypes.EXPLOSION, s.getX(), s.getY() + 1, s.getZ(), 2, 0.5, 0.5, 0.5, 0);
                    s.discard();
                }
            }
            Belt.restoreCenter(lvl);
            lvl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 3, 1, 1, 1, 0);
            lvl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4f, 0.5f);
            lvl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 3f, 1.2f);
        }
        super.die(src);
    }
}
