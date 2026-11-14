package com.pvpciv.combat;

import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.loot.Rarity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 0.11 — единая картотека оружия. Всё, что влияет на баланс, сведено сюда и в {@link CivWeaponItem.WClass}:
 *  • класс задаёт множитель урона за удар, скорость атаки, скорость в паре и дугу (см. WClass, Melee.dualSpeed, Melee.arc);
 *  • тир — базовый урон (Balance.weaponDmg), редкость и заточка — множители;
 *  • особые криты — таблица CRIT ниже (раньше были разбросаны по двум регистрам и дублировались в описаниях).
 * /civ admin weapons — сводная таблица всех стволов: урон за удар, скорость, УВС соло и в паре, урон по броне моба своего этажа.
 */
public final class Weapons {
    private Weapons() {}

    /** Особые криты: id → {шанс %, крит-урон %}. */
    private static final Map<String, int[]> CRIT = Map.ofEntries(
            Map.entry("yuki_katana", new int[]{10, 25}), Map.entry("nodachi", new int[]{3, 10}), Map.entry("kama", new int[]{6, 0}), Map.entry("kunai", new int[]{10, 15}),
            Map.entry("tetsubo", new int[]{0, 25}), Map.entry("kappa_naginata", new int[]{8, 20}), Map.entry("fox_fang", new int[]{12, 10}),
            Map.entry("foxfire_blade", new int[]{6, 20}), Map.entry("nine_tails", new int[]{12, 30}),
            Map.entry("plasma_cutter", new int[]{4, 15}), Map.entry("ion_dagger", new int[]{12, 10}), Map.entry("cryo_saber", new int[]{8, 0}),
            Map.entry("gravity_hammer", new int[]{0, 30}), Map.entry("vine_axe", new int[]{10, 25}));

    public static String id(Item it) { return BuiltInRegistries.ITEM.getKey(it).getPath(); }
    public static int[] crit(Item it) { return CRIT.get(id(it)); }

    /** Все оружия мода по порядку тиров. */
    public static List<CivWeaponItem> all() {
        List<CivWeaponItem> l = new ArrayList<>();
        for (Item it : BuiltInRegistries.ITEM) if (it instanceof CivWeaponItem w) l.add(w);
        l.sort(Comparator.comparingDouble(CivWeaponItem::tierValue).thenComparing(w -> w.cls.ordinal()));
        return l;
    }

    /** Урон за удар (обычная редкость, без заточки). */
    public static double hit(CivWeaponItem w) { return CivWeaponItem.damage(w.tierValue(), w.cls, w.minRarity, 0); }
    /** Ударов в секунду: соло и одной рукой в паре. */
    public static double speed(CivWeaponItem w) { return 4 + w.cls.speed; }
    public static double dualSpeed(CivWeaponItem w) { return speed(w) * Melee.dualSpeed(new ItemStack(w)); }

    /** Сводная таблица (для /civ admin weapons и лога сервера). */
    public static List<String> report() {
        List<String> out = new ArrayList<>();
        out.add(String.format(Locale.ROOT, "%-16s %-6s %-4s %8s %5s %8s %5s %9s %7s %s", "id", "класс", "тир", "удар", "уд/с", "УВС", "пара", "УВС пары", "по бр.", "крит"));
        for (CivWeaponItem w : all()) {
            double h = hit(w), s = speed(w), ds = dualSpeed(w);
            int fl = (int) Math.max(1, Math.min(4, Math.round(w.tierValue())));
            int mobLvl = Math.max(1, fl * 6 - 3);
            double armor = Balance.mobArmor(mobLvl) * w.cls.dmg;
            double afterArmor = Balance.mitigate(h, armor, 0, 0) / h;
            int[] c = crit(w);
            out.add(String.format(Locale.ROOT, "%-16s %-6s %-4s %8s %5.2f %8s %5.2f %9s %6.0f%% %s", id(w), w.cls.title, w.tierName(), Balance.fmt(h), s, Balance.fmt(h * s),
                    ds, Balance.fmt(h * ds * 2), afterArmor * 100, c == null ? "—" : c[0] + "%/+" + c[1] + "%"));
        }
        return out;
    }
}
