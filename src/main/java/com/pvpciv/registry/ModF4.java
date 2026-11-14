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

/** 0.11 — предметы этажа 4 «НТЦ NaZa»: материалы, модули и катализаторы зачарования, оружие и броня тира 4, прибор Стеллы. */
public final class ModF4 {
    private static DeferredItem<com.pvpciv.item.SimpleCivItem> simple(String id, int price, boolean foil, int stack, String... lines) {
        return ITEMS.register(id, () -> new com.pvpciv.item.SimpleCivItem(new Item.Properties().stacksTo(stack), price, foil, lines));
    }

    // ---- материалы (массовые — стак 99)
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> LAZURATRON = simple("lazuratron", 12, false, 99,
            "Лазуратрон — синие кристаллы с нанозарядом.", "Основа молекулярного зачарования (стол Чарователя).");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> NANO_SCRAP = simple("nano_scrap", 9, false, 99, "Нанолом. Шевелится, если не смотреть.", "Материал экзокостюма.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> BIOGEL = simple("biogel", 8, false, 99, "Биогель из Оранжереи.", "Хлам этажа 4.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> CRYO_SHARD = simple("cryo_shard", 10, false, 99, "Осколок криоканистры.", "Хлам этажа 4.");
    // ---- редкие
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> QUANTUM_CHIP = simple("quantum_chip", 110, true, 16, "Квантовый чип NaZa.", "Сердце ядра брони этажа 4 (алтарь).");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> PLASMA_CELL = simple("plasma_cell", 140, true, 16, "Плазменная ячейка. Тёплая.", "Редкий материал этажа 4.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> VINE_HEART = simple("vine_heart", 200, true, 16, "Сердце Лианы. Бьётся.", "Очень редкий материал.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> NAZA_TROPHY = simple("naza_trophy", 0, true, 1, "Трофей: доктор Ноэль и Лиана повержены.", "Для витрины в квартире.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> NAZA_BADGE = simple("naza_badge", 0, true, 1, "Пропуск сотрудника NaZa.", "Даёт Чарователь. Открывает молекулярный стол.");

    // ---- модули молекулярного стола (ставятся ПКМ рядом со столом)
    public static final DeferredItem<com.pvpciv.item.EnchModuleItem> MODULE_BASIC = ITEMS.register("module_basic",
            () -> new com.pvpciv.item.EnchModuleItem(new Item.Properties().stacksTo(16), 1));
    public static final DeferredItem<com.pvpciv.item.EnchModuleItem> MODULE_IMPROVED = ITEMS.register("module_improved",
            () -> new com.pvpciv.item.EnchModuleItem(new Item.Properties().stacksTo(16), 2));
    public static final DeferredItem<com.pvpciv.item.EnchModuleItem> MODULE_ADVANCED = ITEMS.register("module_advanced",
            () -> new com.pvpciv.item.EnchModuleItem(new Item.Properties().stacksTo(16), 3));
    // ---- катализаторы (по желанию)
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> CAT_FOCUS = simple("cat_focus", 40, false, 16, "Катализатор «Фокус».", "Зачарование: +1 вариант на выбор.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> CAT_ANCHOR = simple("cat_anchor", 60, false, 16, "Стабилизатор «Якорь».", "Зачарование: можно оставить старое.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> CAT_RESONATOR = simple("cat_resonator", 80, true, 16, "Резонатор.", "Зачарование: варианты сильнее (+1 ранг).");

    // ---- прибор Стеллы
    public static final DeferredItem<com.pvpciv.item.PhaseAnchorItem> PHASE_ANCHOR = ITEMS.register("phase_anchor",
            () -> new com.pvpciv.item.PhaseAnchorItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    // ---- оружие тира 4
    private static DeferredItem<com.pvpciv.item.CivWeaponItem> w4(String id, boolean plus, WClass c, Rarity min, String lore) {
        return ITEMS.register(id, () -> new com.pvpciv.item.CivWeaponItem(4, plus, c, min, lore));
    }
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> PLASMA_CUTTER = w4("plasma_cutter", false, WClass.SWORD, Rarity.COMMON, "Резак для обшивки.");
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> ION_DAGGER = w4("ion_dagger", false, WClass.DAGGER, Rarity.COMMON, "Ионный стилет.");
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> CRYO_SABER = w4("cryo_saber", false, WClass.SABER, Rarity.COMMON, "Сабля с криоконтуром.");
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> GRAVITY_HAMMER = w4("gravity_hammer", false, WClass.HAMMER, Rarity.COMMON, "Гравимолот.");
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> VINE_AXE = w4("vine_axe", true, WClass.AXE, Rarity.EPIC, "Топор, проросший лианой.");


    // ---- броня тира 4: экзокостюм NaZa
    public static final DeferredHolder<net.minecraft.world.item.ArmorMaterial, net.minecraft.world.item.ArmorMaterial> EXO = ARMOR_MATERIALS.register("exo",
            () -> new net.minecraft.world.item.ArmorMaterial(java.util.Map.of(ArmorItem.Type.HELMET, 0, ArmorItem.Type.CHESTPLATE, 0, ArmorItem.Type.LEGGINGS, 0, ArmorItem.Type.BOOTS, 0, ArmorItem.Type.BODY, 0),
                    0, net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE, () -> net.minecraft.world.item.crafting.Ingredient.EMPTY,
                    java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(PvpCiv.id("exo"))), 0f, 0f));
    private static DeferredItem<com.pvpciv.item.CivArmorItem> exo(String id, ArmorItem.Type t, String lore) {
        return ITEMS.register(id, () -> new com.pvpciv.item.CivArmorItem(EXO, t, 4.0, "Экзокостюм NaZa", lore));
    }
    public static final DeferredItem<com.pvpciv.item.CivArmorItem> EXO_HELMET = exo("exo_helmet", ArmorItem.Type.HELMET, "Шлем с визором. Показывает погоду. Всегда «ясно».");
    public static final DeferredItem<com.pvpciv.item.CivArmorItem> EXO_CHEST = exo("exo_chestplate", ArmorItem.Type.CHESTPLATE, "Сервоприводы гудят в такт сердцу.");
    public static final DeferredItem<com.pvpciv.item.CivArmorItem> EXO_LEGS = exo("exo_leggings", ArmorItem.Type.LEGGINGS, "Шаг — как у робота. Буквально.");
    public static final DeferredItem<com.pvpciv.item.CivArmorItem> EXO_BOOTS = exo("exo_boots", ArmorItem.Type.BOOTS, "Магнитные подошвы. Выключены.");

    public static void init() {}
}
