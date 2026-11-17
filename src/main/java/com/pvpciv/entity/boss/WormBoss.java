package com.pvpciv.entity.boss;

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
            case DASH_AIM -> { hover(dashFrom, 0.7); if (t >= 50) { st = St.DASH; t = 0; dashHit.clear(); lvl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3f, 0.7f); } }
            case DASH -> dashTick(lvl);
            case STUNNED -> {
                if (t % 10 == 0) lvl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 2, getZ(), 10, 1, 0.5, 1, 0.1);
                if (t >= 200) getUp();
            }
        }
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

    private void orbit(ServerLevel lvl) {
        double speed = enraged ? 0.75 : 0.6;
        orbitA += speed / 24.0;
        Vec3 c = c();
        Vec3 to = c.add(Math.cos(orbitA) * 24, 7 + Math.sin(orbitA * 2.3) * 4, Math.sin(orbitA) * 24);
        hover(to, speed * 1.4);
        if (t > (enraged ? 70 : 100)) {
            t = 0;
            actions++;
            List<ServerPlayer> vs = victims(lvl, 60);
            if (vs.isEmpty()) return;
            ServerPlayer target = vs.get(random.nextInt(vs.size()));
            if (actions % 3 == 0) aimDash(lvl, target);
            else if (random.nextBoolean()) startSwallow(lvl, vs);
            else spheres(lvl, vs);
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
        if (t < 40) { hover(at.add(0, 10, 0).add(at.subtract(c()).normalize().scale(6)), 0.8); return; }
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
        dashFrom = c.subtract(dir.scale(26)).add(0, tp.y - c.y + 1.2, 0);
        dashTo = c.add(dir.scale(8)).add(0, 0.5, 0);
        st = St.DASH_AIM;
        t = 0;
        // линия-предупреждение по земле: от края через цель к центру
        for (int i = 0; i <= 17; i++) {
            Vec3 at = dashFrom.lerp(dashTo, i / 17.0);
            final Vec3 a = new Vec3(at.x, tp.y, at.z);
            com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.ZONE, a, 2.0f, 50, com.pvpciv.entity.FxEntity.rgb(RED), light(RED));
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
            if (p.distanceTo(this) > 3.2 || !dashHit.add(p.getUUID())) continue;
            boolean over = Progress.d(p).level >= lvlM + 3;
            p.invulnerableTime = 0;
            p.hurt(damageSources().mobAttack(this), p.getMaxHealth() * (over ? 0.45f : 5f));
            knock(p, position(), 1.5, 0.8);
        }
        if (d.length() < 1.7 || t > 80) {
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
    private void spawnSegments(ServerLevel lvl) {
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
            lvl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 3, 1, 1, 1, 0);
            lvl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4f, 0.5f);
            lvl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 3f, 1.2f);
        }
        super.die(src);
    }
}
