package com.pvpciv.combat;

import com.pvpciv.item.CivWeaponItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 0.10 — своя боёвка.
 *  • Ванильных критов в прыжке нет; ванильного «размаха» нет. Урон зависит от заряда удара (скорости атаки).
 *  • Удар — взмах перед собой: задевает всех врагов в дуге (ширина и дальность — от класса), основная цель — полный урон, остальные — доля.
 *  • Взмах по воздуху тоже бьёт тех, кто в дуге (если заряд ≥ 70%).
 *  • Закликивания нет: незаряженный удар не проходит (клиент держит клик в буфере до готовности руки).
 *  • Два оружия: у каждой руки свой заряд и полный урон своего оружия; в паре каждая рука перезаряжается медленнее
 *    по классу (кинжал ×1, сабля ×0.95, меч ×0.5, топор/молот ×0.3) — кинжалы и сабли созданы для пары, тяжёлое — нет.
 *    В «своём» режиме у любого оружия одного тира урон в секунду примерно равный (~1.6 × урон тира).
 */
public final class Melee {
    private Melee() {}

    /** Дуга удара: дальность, полуугол (°), доля урона по остальным целям. */
    public record Arc(double range, double halfAngle, double others) {}

    public static Arc arc(ItemStack st) {
        if (!(st.getItem() instanceof CivWeaponItem w)) return new Arc(3.0, 45, 0.4);
        return switch (w.cls) {
            case DAGGER -> new Arc(2.7, 30, 0.25);
            case SABER -> new Arc(3.0, 45, 0.4);
            case SWORD -> new Arc(3.2, 50, 0.5);
            case AXE -> new Arc(3.2, 65, 0.7);
            case HAMMER -> new Arc(3.4, 80, 0.8);
        };
    }

    /** 0.10.3: множитель скорости КАЖДОЙ руки в паре (у рук свой заряд; удар второй рукой — полный урон). */
    public static double dualSpeed(ItemStack st) {
        if (!(st.getItem() instanceof CivWeaponItem w)) return 1.0;
        return switch (w.cls) { case DAGGER -> 1.0; case SABER -> 0.95; case SWORD -> 0.5; case AXE -> 0.3; case HAMMER -> 0.3; };
    }

    /** Задержка руки (тики) при полном заряде. */
    public static double delay(LivingEntity p, ItemStack st, boolean dual) {
        double sp = st.getItem() instanceof CivWeaponItem w ? 4 + w.cls.speed : 4;
        if (dual) sp *= dualSpeed(st);
        return 20.0 / Math.max(0.05, sp);
    }

    public static boolean dual(LivingEntity p) {
        return p.getMainHandItem().getItem() instanceof CivWeaponItem && p.getOffhandItem().getItem() instanceof CivWeaponItem;
    }

    // ------------------------------------------------------------------ рука удара (клиент сообщает перед ударом)
    private static final java.util.Map<java.util.UUID, InteractionHand> NEXT = new java.util.HashMap<>();
    /** Каким оружием сейчас идёт удар (во время удара второй рукой — её оружие). */
    public static ItemStack striking(ServerPlayer p) { return NEXT.get(p.getUUID()) == InteractionHand.OFF_HAND ? p.getOffhandItem() : p.getMainHandItem(); }

    public static void setHand(ServerPlayer p, boolean off) { NEXT.put(p.getUUID(), off ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND); }
    public static InteractionHand hand(ServerPlayer p) { return dual(p) ? NEXT.getOrDefault(p.getUUID(), InteractionHand.MAIN_HAND) : InteractionHand.MAIN_HAND; }

    /** Множитель урона прямого удара игрока: обе руки — полный урон; 0.10.4 — бонус комбо (+6% за стак, до 5). */
    public static double handMul(ServerPlayer p) { int c = combo(p); return 1.0 + 0.03 * c; }   // 0.10.5: +3% за стак, до ×10

    // ------------------------------------------------------------------ 0.10.4: комбо (точный удар в окно готовности)
    public static final int COMBO_MAX = 10;
    private record Combo(int n, long tick) {}
    private static final java.util.Map<java.util.UUID, Combo> COMBO = new java.util.HashMap<>();
    /** Клиент сообщает перед ударом: n > 0 — удар точный, n — текущий стак. */
    public static void setCombo(ServerPlayer p, int n) { COMBO.put(p.getUUID(), new Combo(Math.max(0, Math.min(COMBO_MAX, n)), p.serverLevel().getGameTime())); }
    /** Стак комбо текущего удара (действует 3 тика — на удар и его дугу). */
    public static int combo(ServerPlayer p) {
        Combo c = COMBO.get(p.getUUID());
        return c == null || p.serverLevel().getGameTime() - c.tick > 3 ? 0 : c.n;
    }

    // ------------------------------------------------------------------ 0.10.8: полный урон при заряженном ударе
    private record Charge(float s, long tick) {}
    private static final java.util.Map<java.util.UUID, Charge> CHARGE = new java.util.HashMap<>();
    public static void noteCharge(ServerPlayer p, float s) { CHARGE.put(p.getUUID(), new Charge(s, p.serverLevel().getGameTime())); }

    /**
     * Ваниль режет урон удара основной рукой по заряду: ×(0.2 + 0.8·s²). Клик из буфера и рассинхрон клиента/сервера на тик
     * давали 85–90% урона. Если удар заряжен хотя бы на 85% — возвращаем полный урон.
     */
    public static double chargeFix(ServerPlayer p, net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e) {
        if (e.getSource().getDirectEntity() != p || sweeping) return 1.0;
        Charge c = CHARGE.get(p.getUUID());
        if (c == null || p.serverLevel().getGameTime() - c.tick > 1 || c.s >= 0.999f || c.s < 0.6f) return 1.0;   // клиент пускает удар только с 92%; сервер может отставать на тики
        return 1.0 / (0.2 + 0.8 * c.s * c.s);
    }

    /** Урон оружия руки с бонусами игрока (титулы, вехи — они висят на атрибуте урона основной руки). */
    public static double handDamage(ServerPlayer p, ItemStack st) {
        double raw = st.getItem() instanceof CivWeaponItem w ? w.damage(st) : 1.0;
        ItemStack main = p.getMainHandItem();
        double mainRaw = main.getItem() instanceof CivWeaponItem mw ? mw.damage(main) : 1.0;
        double attr = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        return raw * Math.max(1.0, attr / Math.max(0.01, mainRaw));
    }

    // ------------------------------------------------------------------ вторая рука: свой заряд, удар без ванильной атаки
    private static final java.util.Map<java.util.UUID, Long> OFF_LAST = new java.util.HashMap<>();

    public static float offCharge(ServerPlayer p) {
        Long last = OFF_LAST.get(p.getUUID());
        if (last == null) return 1f;
        double d = delay(p, p.getOffhandItem(), true);
        return (float) Math.min(1, (p.serverLevel().getGameTime() - last + 0.5) / d);
    }

    /** 0.10.5: руки чередуются ровно — после удара одной рукой заряд другой не выше 50% (окна идут через полпериода). */
    private static java.lang.reflect.Field TICKER;
    public static void capMain(LivingEntity p, double frac) {
        try {
            if (TICKER == null) { TICKER = LivingEntity.class.getDeclaredField("attackStrengthTicker"); TICKER.setAccessible(true); }
            double d = delay(p, p.getMainHandItem(), true);
            int cap = (int) Math.floor(d * frac);
            if (TICKER.getInt(p) > cap) TICKER.setInt(p, cap);
        } catch (Exception ignored) {}
    }
    /** После удара основной рукой: заряд второй — не выше 50%. */
    public static void afterMain(ServerPlayer p) {
        if (!dual(p)) return;
        long now = p.serverLevel().getGameTime();
        double d = delay(p, p.getOffhandItem(), true);
        long cap = now - (long) Math.floor(d * 0.5);
        OFF_LAST.merge(p.getUUID(), cap, Math::max);
    }

    /** Удар второй рукой по сущности (id) или по воздуху (id < 0). */
    public static void offHit(ServerPlayer p, int id) {
        if (!dual(p) || p.isSpectator() || offCharge(p) < 0.6f) return;
        OFF_LAST.put(p.getUUID(), p.serverLevel().getGameTime());
        capMain(p, 0.5);
        ItemStack st = p.getOffhandItem();
        double dmg = handDamage(p, st);   // 0.10.8: с бонусами титулов и вех, как у основной руки
        NEXT.put(p.getUUID(), InteractionHand.OFF_HAND);
        p.swing(InteractionHand.OFF_HAND, true);
        slash(p, st, true);
        try {
            net.minecraft.world.entity.Entity t = id >= 0 ? p.serverLevel().getEntity(id) : null;
            if (t instanceof LivingEntity le && le.isAlive() && p.distanceToSqr(le) < 6.5 * 6.5) {
                le.hurt(p.damageSources().playerAttack(p), (float) dmg);
            } else hitArc(p, arc(st), dmg, null);
        } finally { NEXT.put(p.getUUID(), InteractionHand.MAIN_HAND); }
    }

    /** Всем вокруг — показать взмах (шлейф) этого игрока. */
    public static void slash(ServerPlayer p, ItemStack st, boolean off) {
        int cls = st.getItem() instanceof CivWeaponItem w ? w.cls.ordinal() : 0;
        int col = com.pvpciv.loot.Gear.rarity(st).color;
        String js = "{\"e\":" + p.getId() + ",\"k\":" + cls + ",\"o\":" + off + ",\"c\":" + col + "}";
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(p, new com.pvpciv.net.Network.PhoneData(17, js));
    }

    // ------------------------------------------------------------------ дуга
    private static boolean sweeping;

    /** После прямого удара по primary: задеть остальных в дуге. */
    public static void cleave(ServerPlayer p, LivingEntity primary, float dealt) {
        if (sweeping || dealt <= 0) return;
        ItemStack st = hand(p) == InteractionHand.OFF_HAND ? p.getOffhandItem() : p.getMainHandItem();
        Arc a = arc(st);
        hitArc(p, a, dealt * a.others, primary);
    }

    /** Взмах по воздуху (пакет от клиента): по всем в дуге — полный урон × заряд. */
    public static void airSwing(ServerPlayer p) {
        float charge = p.getAttackStrengthScale(0.5f);
        if (charge < 0.6f || p.isSpectator()) return;
        ItemStack st = p.getMainHandItem();
        Arc a = arc(st);
        double base = p.getAttributeValue(Attributes.ATTACK_DAMAGE) * (charge >= 0.6f ? 1.0 : 0.2 + 0.8 * charge * charge);   // 0.10.8: заряжен — полный
        p.resetAttackStrengthTicker();
        afterMain(p);
        p.swing(InteractionHand.MAIN_HAND, true);
        slash(p, st, false);
        hitArc(p, a, base, null);
    }

    private static void hitArc(ServerPlayer p, Arc a, double dmg, LivingEntity skip) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
        double cos = Math.cos(Math.toRadians(a.halfAngle));
        sweeping = true;
        try {
            int n = 0;
            for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(a.range + 1), x -> x != p && x != skip && x.isAlive())) {
                if (!(e instanceof net.minecraft.world.entity.monster.Enemy) && Mobs.level(e) <= 0 && !(e instanceof ServerPlayer)) continue;
                Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
                double d = to.length();
                if (d > a.range + e.getBbWidth() * 0.5) continue;
                Vec3 tf = new Vec3(to.x, 0, to.z);
                if (tf.lengthSqr() > 0.04 && tf.normalize().dot(flat) < cos) continue;
                if (!p.hasLineOfSight(e)) continue;
                e.hurt(p.damageSources().playerAttack(p), (float) dmg);
                if (++n >= 6) break;
            }
            if (n > 0) p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                    p.getX() + flat.x * 1.4, p.getY(0.55), p.getZ() + flat.z * 1.4, 1, 0, 0, 0, 0);
        } finally { sweeping = false; }
    }

    public static boolean isSweeping() { return sweeping; }

    // ------------------------------------------------------------------ скорость атаки в паре
    private static final ResourceLocation DUAL_ID = com.pvpciv.PvpCiv.id("dual_speed");

    public static void tick(MinecraftServer s) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            var inst = p.getAttribute(Attributes.ATTACK_SPEED);
            if (inst == null) continue;
            double k = dual(p) ? dualSpeed(p.getMainHandItem()) : 1.0;
            var cur = inst.getModifier(DUAL_ID);
            if (k == 1.0) { if (cur != null) inst.removeModifier(DUAL_ID); continue; }
            if (cur != null && Math.abs(cur.amount() - (k - 1)) < 1e-6) continue;
            inst.removeModifier(DUAL_ID);
            inst.addTransientModifier(new AttributeModifier(DUAL_ID, k - 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
