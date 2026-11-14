package com.pvpciv.combat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.ItemStack;

/**
 * 0.9.9 — своя шкала чисел. Ванильные потолки (HP 1024, урон 2048, броня 30) сняты; броня считается своей формулой.
 * Ориентир: мобы этажа 3 — сотни HP, боссы — тысячи; к этажу 10 — миллионы.
 */
public final class Balance {
    private Balance() {}

    // ------------------------------------------------------------ мобы
    public static double mobHp(int lvl) { return 30 * Math.pow(1.2, lvl - 1); }
    public static double mobDmg(int lvl) { return 4 * Math.pow(1.19, lvl - 1); }
    /** Старая шкала (до 0.9.9) — для пересчёта уже живущих мобов. */
    public static double oldHp(int lvl) { return 20 * Math.pow(1.065, lvl - 1); }
    public static double oldDmg(int lvl) { return 3 * Math.pow(1.07, lvl - 1); }

    // ------------------------------------------------------------ игрок
    /** Полное базовое HP игрока на уровне (включая ванильные 20). */
    public static double playerHp(int lvl) { return 30 * Math.pow(1.18, lvl - 1); }
    public static double weaponDmg(double tier) { return 7 * Math.pow(2.4, tier - 1); }
    /** Броня (число) и HP за полный комплект тира (части делят по долям). 0.10.2: броня — вычитается из удара. */
    public static double armorDef(double tier) { return 1.7 * Math.pow(2.4, tier - 1); }
    public static double armorHp(double tier) { return 12 * Math.pow(2.2, tier - 1.5); }
    /** Прибавка брони игрока за уровень. */
    public static double levelArmor(int lvl) { return 0.4 * Math.pow(1.17, lvl - 1); }
    /** Броня моба уровня L (обычного типа). */
    public static double mobArmor(int lvl) { return 0.35 * Math.pow(1.2, lvl - 1); }

    /** Броня мода на вещах (без уровня). */
    public static double gearArmor(LivingEntity e) {
        double s = 0;
        for (EquipmentSlot sl : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = e.getItemBySlot(sl);
            if (st.getItem() instanceof com.pvpciv.item.CivArmorItem a) s += a.armor(st);
        }
        return s;
    }
    /** Совместимость: «броня» игрока без уровня (HUD до синхронизации). */
    public static double defense(LivingEntity e) { return gearArmor(e); }

    /** Полная броня: у игрока — вещи + уровень, у моба — от уровня и типа (боссы ×2, элиты ×1.5). */
    public static double armor(LivingEntity e, int playerLevel) {
        if (e instanceof net.minecraft.world.entity.player.Player) return gearArmor(e) + levelArmor(playerLevel);
        int l = Mobs.level(e);
        if (l <= 0) return 0;
        double k = 1;
        Mobs.Kind kind = Mobs.Kind.of(e.getType());
        if (kind != null) k = switch (kind) {
            case SAMURAI, POLICE, GUNNER, FROST_ZOMBIE, SENTRY, ROBOT, TURRET -> 1.5;
            case RAT, FOX, STREET_DOG, ASH_SPIDER, FROST_SPIDER, NINJA, SPORELING, BOTANIST -> 0.6;
            default -> 1.0;
        };
        if (e instanceof com.pvpciv.entity.boss.CivBoss) k = 1.5;
        else if (Mobs.lootKind(e).endsWith("_elite")) k *= 1.5;
        return mobArmor(l) * k;
    }

    /** Защита (%) — второй слой: полный комплект одного сета даёт 10%. Потолок 50%. */
    public static double protection(LivingEntity e) {
        String set = null; int n = 0;
        for (EquipmentSlot sl : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = e.getItemBySlot(sl);
            if (st.getItem() instanceof com.pvpciv.item.CivArmorItem a) { if (set == null) set = a.set; if (set.equals(a.set)) n++; }
        }
        double p = n == 4 ? 0.10 : 0;
        return Math.min(0.5, p);
    }

    /** Пробивание текущего удара (0…1): доля брони, которую он игнорирует. Ставится на время удара (пули — 50%). */
    public static double PEN = 0;

    /**
     * Итог удара по броне: броня ≥ 2× урона — иммунитет (0); иначе урон − броня, но не меньше 20% удара; затем защита %.
     */
    public static double mitigate(double dmg, double armor, double pen, double prot) {
        if (dmg <= 0) return 0;
        double a = armor * (1 - Math.max(0, Math.min(1, pen)));
        if (a >= dmg * 2) return 0;
        double d = Math.max(dmg - a, dmg * 0.2);
        return d * (1 - prot);
    }

    /** Старая процентная формула (не используется с 0.10.2). */
    public static double reduction(double def, int attackerLevel) {
        if (def <= 0) return 0;
        double k = 8 * Math.pow(1.19, Math.max(1, attackerLevel) - 1);
        return Math.min(0.8, def / (def + k));
    }

    // ------------------------------------------------------------ числа
    public static String fmt(double v) {
        double a = Math.abs(v);
        if (a < 10) return String.format(java.util.Locale.ROOT, "%.1f", v);
        if (a < 10_000) return String.valueOf(Math.round(v));
        if (a < 1_000_000) return trim(v / 1000) + "k";
        if (a < 1_000_000_000) return trim(v / 1_000_000) + "M";
        return trim(v / 1_000_000_000) + "B";
    }
    private static String trim(double v) { return Math.abs(v) >= 100 ? String.valueOf(Math.round(v)) : String.format(java.util.Locale.ROOT, "%.1f", v); }

    /** Снять ванильные потолки атрибутов (HP 1024, урон 2048). */
    public static void liftCaps() {
        lift(Attributes.MAX_HEALTH.value());
        lift(Attributes.ATTACK_DAMAGE.value());
    }
    private static void lift(net.minecraft.world.entity.ai.attributes.Attribute a) {
        if (!(a instanceof RangedAttribute r)) return;
        try {
            java.lang.reflect.Field f = RangedAttribute.class.getDeclaredField("maxValue");
            f.setAccessible(true);
            f.setDouble(r, 1.0e12);
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().error("[pvpciv] не удалось снять потолок атрибута {}", a.getDescriptionId(), e);
        }
    }
}
