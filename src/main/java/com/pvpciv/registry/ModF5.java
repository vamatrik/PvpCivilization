package com.pvpciv.registry;

import com.pvpciv.PvpCiv;
import com.pvpciv.item.CivWeaponItem.WClass;
import com.pvpciv.loot.Rarity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import static com.pvpciv.registry.ModRegistry.ARMOR_MATERIALS;
import static com.pvpciv.registry.ModRegistry.ITEMS;

/** 0.12 — предметы этажа 5 «Станция «Орбита»»: материалы, сжатый лазуратрон, оружие и броня тира 5. */
public final class ModF5 {
    private static DeferredItem<com.pvpciv.item.SimpleCivItem> simple(String id, int price, boolean foil, int stack, String... lines) {
        return ITEMS.register(id, () -> new com.pvpciv.item.SimpleCivItem(new Item.Properties().stacksTo(stack), price, foil, lines));
    }

    /** Сжатый лазуратрон = 8 обычных. В столе зачарования тратится как 8, сдача возвращается обычным. */
    public static final int CMP = 8;
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> CMP_LAZURATRON = simple("cmp_lazuratron", 110, true, 99,
            "Сжатый лазуратрон — 8 обычных в одном кристалле.", "Стол зачарования берёт его как 8, сдачу вернёт обычным.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> STAR_ALLOY = simple("star_alloy", 14, false, 99, "Звёздный сплав. Ковали в невесомости.", "Материал скафандра «Орбита».");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> VOID_SHARD = simple("void_shard", 12, false, 99, "Осколок пустоты. Холодный изнутри.", "Хлам этажа 5.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> CIRCUIT = simple("circuit", 10, false, 99, "Плата со станции. Кто-то её погрыз.", "Хлам этажа 5.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> ASTRO_CORE = simple("astro_core", 160, true, 16, "Астроядро.", "Сердце ядра брони этажа 5 (алтарь).");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> QUASAR_CELL = simple("quasar_cell", 220, true, 16, "Квазарная ячейка. Светится сквозь пальцы.", "Редкий материал этажа 5.");
    public static final DeferredItem<com.pvpciv.item.BagBuckleItem> GRAV_POUCH = ITEMS.register("grav_pouch",
            () -> new com.pvpciv.item.BagBuckleItem(new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.EPIC), 5, 5));

    // ---- оружие тира 5
    private static DeferredItem<com.pvpciv.item.CivWeaponItem> w5(String id, boolean plus, WClass c, Rarity min, String lore) {
        return ITEMS.register(id, () -> new com.pvpciv.item.CivWeaponItem(5, plus, c, min, lore));
    }
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> PHOTON_BLADE = w5("photon_blade", false, WClass.SWORD, Rarity.COMMON, "Фотонный клинок. Светит ярче фонарика.");
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> VOID_KNIFE = w5("void_knife", false, WClass.DAGGER, Rarity.COMMON, "Нож пустоты. Режет даже тишину.");
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> COMET_SABER = w5("comet_saber", false, WClass.SABER, Rarity.COMMON, "Сабля «Комета». Оставляет хвост.");
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> METEOR_AXE = w5("meteor_axe", false, WClass.AXE, Rarity.COMMON, "Метеоритный топор. Тяжёлый, как падение.");
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> SUPERNOVA = w5("supernova", true, WClass.HAMMER, Rarity.EPIC, "Молот «Сверхновая». Один удар — одна звезда.");

    // ---- броня тира 5: скафандр «Орбита»
    public static final DeferredHolder<net.minecraft.world.item.ArmorMaterial, net.minecraft.world.item.ArmorMaterial> ORBIT = ARMOR_MATERIALS.register("orbit",
            () -> new net.minecraft.world.item.ArmorMaterial(java.util.Map.of(ArmorItem.Type.HELMET, 0, ArmorItem.Type.CHESTPLATE, 0, ArmorItem.Type.LEGGINGS, 0, ArmorItem.Type.BOOTS, 0, ArmorItem.Type.BODY, 0),
                    0, net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE, () -> net.minecraft.world.item.crafting.Ingredient.EMPTY,
                    java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(PvpCiv.id("orbit"))), 0f, 0f));
    private static DeferredItem<com.pvpciv.item.CivArmorItem> orbit(String id, ArmorItem.Type t, String lore) {
        return ITEMS.register(id, () -> new com.pvpciv.item.CivArmorItem(ORBIT, t, 5.0, "Скафандр «Орбита»", lore));
    }
    public static final DeferredItem<com.pvpciv.item.CivArmorItem> ORBIT_HELMET = orbit("orbit_helmet", ArmorItem.Type.HELMET, "Золотое стекло. В нём отражаются звёзды.");
    public static final DeferredItem<com.pvpciv.item.CivArmorItem> ORBIT_CHEST = orbit("orbit_chestplate", ArmorItem.Type.CHESTPLATE, "Ранец с кислородом на 8 часов. Или на 3 боя.");
    public static final DeferredItem<com.pvpciv.item.CivArmorItem> ORBIT_LEGS = orbit("orbit_leggings", ArmorItem.Type.LEGGINGS, "Гофра на коленях. Очень модно.");
    public static final DeferredItem<com.pvpciv.item.CivArmorItem> ORBIT_BOOTS = orbit("orbit_boots", ArmorItem.Type.BOOTS, "Магнитные подошвы. Включены.");

    // ---- 1.0: испытания Луны, классы, финал
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> DARK_MATTER = simple("dark_matter", 0, true, 64,
            "Тёмная материя. Тяжелее, чем выглядит.", "Первое прохождение стадии испытания Луны.", "Тратится в дереве классов (Навыки → «Класс»).");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> DARK_STAR = simple("dark_star", 0, true, 16,
            "Тёмная звезда. Свет вокруг неё гнётся.", "Кузница: заточка +10 → +11 (шанс 35%).", "Неудача — звезда сгорает, оружие цело.");
    public static final DeferredItem<com.pvpciv.item.ForgetScrollItem> FORGET_SCROLL = ITEMS.register("forget_scroll",
            () -> new com.pvpciv.item.ForgetScrollItem(new Item.Properties().stacksTo(4).rarity(net.minecraft.world.item.Rarity.EPIC)));
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> HELL_TRAIL = simple("hell_trail", 0, true, 16,
            "Адский след. Ещё тёплый.", "Демоница Пустоты ушла в Ад и оставила его.", "Чинит Небесный лифт станции (атриум, пульт у шахты).");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> CONTRACT = simple("contract", 0, false, 1, "«Взаимовыгодное соглашение». Мелкий шрифт — на 40 страниц.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> DIPLOMAT_TROPHY = simple("diplomat_trophy", 0, true, 1, "Трофей: Инопланетный дипломат отозван.", "Открывает шлюз ангара — к поясу астероидов.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> WORM_TROPHY = simple("worm_trophy", 0, true, 1, "Трофей: Галактический червь пал.", "Демоница Пустоты ещё вернётся.");

    public static void init() {}
}
