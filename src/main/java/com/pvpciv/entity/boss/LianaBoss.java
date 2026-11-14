package com.pvpciv.entity.boss;

import com.pvpciv.net.Network;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.*;

/**
 * Лиана — хищное растение доктора Ноэля (финальный босс этажа 4, «Ковчег»). Растёт в кадке и не ходит.
 *  • Хлёст — конус лиан перед собой. • Удар сверху — цепочка кругов к цели. • Корни — круги под всеми игроками, обездвиживают.
 *  • Проглатывание — короткий телеграф под целью; кто не ушёл — во рту: жми пробел быстрее, чем тает полоса, урон каждые 0.5 с.
 *    Пока кого-то жуёт — Ноэль не стреляет, чёрные дыры проглоченного не тянут.
 *  • Пока Лиана жива — Ноэль неуязвим. Жизни связаны общей полосой.
 */
public class LianaBoss extends CivBoss {
    private static final Vector3f VINE = new Vector3f(0.35f, 0.85f, 0.2f), ACID = new Vector3f(0.75f, 1f, 0.2f), MAW = new Vector3f(1f, 0.25f, 0.4f);
    /** Проглоченные: игрок → состояние. */
    public static final Map<UUID, Grab> GRABBED = new HashMap<>();
    public static final class Grab { public final UUID liana; public float bar = 0.35f; public int ticks; Grab(UUID l) { liana = l; } }

    public LianaBoss(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Override public String defId() { return "noel"; }
    @Override public String model() { return "liana"; }
    @Override public float renderScale() { return 1.6f; }
    @Override protected String[] actions() { return new String[]{"attack", "lash", "slam", "swallow", "chew", "roar"}; }
    @Override public boolean isPushable() { return false; }
    @Override public void knockback(double s, double x, double z) {}

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 20f));
        targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.player.Player.class, false));
    }

    public static boolean grabbed(ServerPlayer p) { return GRABBED.containsKey(p.getUUID()); }
    public boolean chewing() { for (Grab g : GRABBED.values()) if (g.liana.equals(getUUID())) return true; return false; }

    private Vec3 mouth() { return position().add(getLookAngle().multiply(1, 0, 1).normalize().scale(1.2)).add(0, 4.2, 0); }

    @Override
    protected void ability(ServerLevel lvl, ServerPlayer target) {
        if (chewing()) { cooldown = 10; return; }
        int r = random.nextInt(10);
        double d = distanceTo(target);
        if (d < 7 && r < 3 && !grabbed(target)) swallow(lvl, target);
        else if (d < 8 && r < 6) lash(lvl, target);
        else if (r < 8) slam(lvl, target);
        else roots(lvl);
        cooldown = enraged ? 35 + random.nextInt(15) : 55 + random.nextInt(25);
    }

    private void lash(ServerLevel lvl, ServerPlayer t) {
        faceTo(t.position());
        play("lash", 22);
        lvl.playSound(null, blockPosition(), SoundEvents.VINE_STEP, SoundSource.HOSTILE, 2f, 0.6f);
        Vec3 c = position();
        zone(lvl, c, 8.5, 16, VINE).cone(Math.atan2(t.getZ() - c.z, t.getX() - c.x), Math.toRadians(70))
                .onHit(v -> { if (!grabbed(v)) { v.hurt(damageSources().mobAttack(this), dmg() * 1.2f); knock(v, c, 1.1, 0.45); } });
    }

    private void slam(ServerLevel lvl, ServerPlayer t) {
        faceTo(t.position());
        play("slam", 26);
        Vec3 c = position(), dir = t.position().subtract(c).multiply(1, 0, 1).normalize();
        for (int i = 0; i < 5; i++) {
            Vec3 at = c.add(dir.scale(2.5 + i * 2.6));
            later(i * 3, () -> zone(lvl, at, 1.9, 18, ACID).onHit(v -> { if (!grabbed(v)) { v.hurt(damageSources().mobAttack(this), dmg() * 1.1f); v.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1)); } })
                    .fx(p -> lvl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.x, p.y + 0.4, p.z, 25, 0.8, 0.3, 0.8, 0.02)));
        }
    }

    private void roots(ServerLevel lvl) {
        play("roar", 28);
        castYaw = getYRot();
        lvl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2f, 1.3f);
        for (ServerPlayer p : victims(lvl, 26)) {
            if (grabbed(p)) continue;
            zone(lvl, p.position(), 2.4, 26, VINE).onHit(v -> {
                if (grabbed(v)) return;
                v.hurt(damageSources().mobAttack(this), dmg() * 0.8f);
                v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 6, false, false));
                v.addEffect(new MobEffectInstance(MobEffects.JUMP, 50, 128, false, false));
                v.displayClientMessage(Component.literal("Корни держат!").withStyle(ChatFormatting.GREEN), true);
            });
        }
    }

    /** Для теста (/civ admin grab): проглотить игрока сразу. */
    public void forceGrab(ServerPlayer t) { GRABBED.put(t.getUUID(), new Grab(getUUID())); }

    private void swallow(ServerLevel lvl, ServerPlayer t) {
        faceTo(t.position());
        play("swallow", 20);
        lvl.playSound(null, blockPosition(), SoundEvents.SNIFFER_EAT, SoundSource.HOSTILE, 2f, 0.5f);
        zone(lvl, t.position(), 2.2, 14, MAW).onHit(v -> {
            if (grabbed(v) || chewing() || !isAlive()) return;
            GRABBED.put(v.getUUID(), new Grab(getUUID()));
            v.displayClientMessage(Component.literal("ТЕБЯ ПРОГЛОТИЛИ! ЖМИ ПРОБЕЛ!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
            lvl.playSound(null, blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.HOSTILE, 2f, 0.4f);
        });
    }

    /** Нажатие пробела во рту. */
    public static void mash(ServerPlayer p) {
        Grab g = GRABBED.get(p.getUUID());
        if (g != null) g.bar = Math.min(1f, g.bar + 0.08f);
    }

    @Override
    protected void passive(ServerLevel lvl) {
        // проглоченные: держим во рту, полоса тает, урон каждые 10 тиков
        Iterator<Map.Entry<UUID, Grab>> it = GRABBED.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            Grab g = en.getValue();
            if (!g.liana.equals(getUUID())) continue;
            ServerPlayer p = lvl.getServer().getPlayerList().getPlayer(en.getKey());
            if (p == null || !p.isAlive() || p.level() != lvl) { it.remove(); if (p != null) send(p, null); continue; }
            g.ticks++;
            g.bar -= enraged ? 0.011f : 0.009f;
            Vec3 m = mouth();
            p.teleportTo(m.x, m.y - 1.2, m.z);
            p.setDeltaMovement(0, 0, 0);
            p.fallDistance = 0;
            if (g.ticks % 12 == 0) triggerAnim("main", "chew");
            if (g.ticks % 10 == 0) {
                p.invulnerableTime = 0;
                p.hurt(damageSources().mobAttack(this), dmg() * 0.4f);
                lvl.sendParticles(ParticleTypes.ITEM_SLIME, m.x, m.y, m.z, 8, 0.4, 0.3, 0.4, 0.05);
            }
            if (g.bar >= 1f || g.ticks > 160 || g.bar <= -0.6f) {   // вырвался — или выплюнули через 8 с
                it.remove();
                boolean escaped = g.bar >= 1f;
                Vec3 out = position().add(getLookAngle().multiply(1, 0, 1).normalize().scale(4.5));
                p.teleportTo(out.x, ground(lvl, out) + 0.2, out.z);
                knock(p, position(), escaped ? 0.9 : 1.4, 0.5);
                if (!escaped) p.hurt(damageSources().mobAttack(this), dmg() * 1.5f);
                p.displayClientMessage(Component.literal(escaped ? "Вырвался!" : "Тебя выплюнули…").withStyle(escaped ? ChatFormatting.GREEN : ChatFormatting.DARK_RED), true);
                if (escaped) { hurt(damageSources().playerAttack(p), getMaxHealth() * 0.03f); triggerAnim("main", "roar"); }
                send(p, null);
                continue;
            }
            send(p, g);
        }
    }

    private static void send(ServerPlayer p, Grab g) {
        String js = g == null ? "{\"on\":false}" : String.format(Locale.ROOT, "{\"on\":true,\"bar\":%.3f,\"t\":%d}", Math.max(0, g.bar), g.ticks);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(20, js));
    }

    @Override
    public void remove(RemovalReason r) {
        if (level() instanceof ServerLevel lvl) {
            GRABBED.entrySet().removeIf(en -> {
                if (!en.getValue().liana.equals(getUUID())) return false;
                ServerPlayer p = lvl.getServer().getPlayerList().getPlayer(en.getKey());
                if (p != null) send(p, null);
                return true;
            });
        }
        super.remove(r);
    }

    @Override
    protected void onEnrage(ServerLevel lvl) {
        play("roar", 30);
        for (ServerPlayer p : victims(lvl, 40))
            p.sendSystemMessage(Component.literal("☠ Лиана выпускает шипы! Ноэль: «Нет-нет-нет, дорогая, спокойно!»").withStyle(ChatFormatting.GREEN));
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        // своё не ранит; проглоченный бьёт изнутри вдвое сильнее
        if (src.getEntity() instanceof ServerPlayer p && grabbed(p)) amount *= 2;
        return super.hurt(src, amount);
    }
}
