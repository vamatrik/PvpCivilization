package com.pvpciv.registry;

import com.pvpciv.PvpCiv;
import com.pvpciv.block.ApartmentDoorBlock;
import com.pvpciv.block.ArenaCoreBlock;
import com.pvpciv.data.StoryData;
import com.pvpciv.entity.GuardEntity;
import com.pvpciv.entity.LunaEntity;
import com.pvpciv.entity.StoryNpc;
import com.pvpciv.item.KeyCardItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PvpCiv.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PvpCiv.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, PvpCiv.MODID);
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, PvpCiv.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PvpCiv.MODID);
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, PvpCiv.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, PvpCiv.MODID);

    // --- Data components
    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<Integer>> RARITY =
            COMPONENTS.registerComponentType("rarity", b -> b.persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<Integer>> STARS =
            COMPONENTS.registerComponentType("stars", b -> b.persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    // --- Blocks
    public static final DeferredBlock<ApartmentDoorBlock> APARTMENT_DOOR = BLOCKS.register("apartment_door",
            () -> new ApartmentDoorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(-1.0F, 3600000.0F).noLootTable()));
    public static final DeferredBlock<ArenaCoreBlock> ARENA_CORE = BLOCKS.register("arena_core",
            () -> new ArenaCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LODESTONE).strength(-1.0F, 3600000.0F)
                    .lightLevel(s -> 13).sound(SoundType.LODESTONE).noLootTable()));

    // --- Items
    public static final DeferredItem<KeyCardItem> KEY_CARD = ITEMS.register("key_card",
            () -> new KeyCardItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<SwordItem> ASH_BLADE = ITEMS.register("ash_blade",
            () -> new SwordItem(Tiers.STONE, new Item.Properties()
                    .attributes(SwordItem.createAttributes(Tiers.STONE, 4, -2.3F))
                    .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                    .rarity(Rarity.UNCOMMON)));
    public static final DeferredBlock<com.pvpciv.block.ForgeBlock> FORGE = BLOCKS.register("forge",
            () -> new com.pvpciv.block.ForgeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE).strength(-1.0F, 3600000.0F).lightLevel(s -> 9).noLootTable().noOcclusion()));
    public static final DeferredBlock<com.pvpciv.block.StashBlock> STASH = BLOCKS.register("stash",
            () -> new com.pvpciv.block.StashBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(-1.0F, 3600000.0F).noLootTable()));
    public static final DeferredBlock<com.pvpciv.block.DismantleBlock> DISMANTLE = BLOCKS.register("dismantle_table",
            () -> new com.pvpciv.block.DismantleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FLETCHING_TABLE).strength(-1.0F, 3600000.0F).noLootTable()));
    public static final DeferredItem<BlockItem> DISMANTLE_ITEM = ITEMS.registerSimpleBlockItem(DISMANTLE);
    public static final DeferredItem<Item> DORM_STEW = ITEMS.register("dorm_stew", () -> new Item(new Item.Properties().stacksTo(16)
            .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(7).saturationModifier(0.7f).build())));
    public static final DeferredItem<com.pvpciv.item.BagBuckleItem> BAG_BUCKLE = ITEMS.register("bag_buckle",
            () -> new com.pvpciv.item.BagBuckleItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final DeferredItem<BlockItem> FORGE_ITEM = ITEMS.registerSimpleBlockItem(FORGE);
    public static final DeferredItem<BlockItem> STASH_ITEM = ITEMS.registerSimpleBlockItem(STASH);

    // --- Оружие тира 1
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> NOVICE_SWORD = ITEMS.register("novice_sword",
            () -> new com.pvpciv.item.CivWeaponItem(1, com.pvpciv.item.CivWeaponItem.WClass.SWORD, com.pvpciv.loot.Rarity.COMMON, "«Деревянный, неломаемый. Как мой оптимизм.» — Тим"));
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> RUSTY_CLEAVER = ITEMS.register("rusty_cleaver",
            () -> new com.pvpciv.item.CivWeaponItem(1, com.pvpciv.item.CivWeaponItem.WClass.SWORD, com.pvpciv.loot.Rarity.COMMON, "Ржавчина — это тоже урон. Немного."));
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> ASH_SABER = ITEMS.register("ash_saber",
            () -> new com.pvpciv.item.CivWeaponItem(1, com.pvpciv.item.CivWeaponItem.WClass.SABER, com.pvpciv.loot.Rarity.COMMON, "Быстрая и лёгкая, как пепел на ветру."));
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> BONE_DAGGER = ITEMS.register("bone_dagger",
            () -> new com.pvpciv.item.CivWeaponItem(1, com.pvpciv.item.CivWeaponItem.WClass.DAGGER, com.pvpciv.loot.Rarity.COMMON, "Чья это была кость — лучше не спрашивать."));
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> CHARRED_AXE = ITEMS.register("charred_axe",
            () -> new com.pvpciv.item.CivWeaponItem(1, com.pvpciv.item.CivWeaponItem.WClass.AXE, com.pvpciv.loot.Rarity.COMMON, "Медленный. Тяжёлый. Убедительный."));
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> BRUTE_CLEAVER = ITEMS.register("brute_cleaver",
            () -> new com.pvpciv.item.CivWeaponItem(1, com.pvpciv.item.CivWeaponItem.WClass.HAMMER, com.pvpciv.loot.Rarity.EPIC, "Фирменное оружие Гнилого Громилы. Пахнет соответственно."));

    // --- Материалы, хлам, звёзды
    public static final DeferredItem<com.pvpciv.item.SparkItem> SPARKS = ITEMS.register("sparks", () -> new com.pvpciv.item.SparkItem(new Item.Properties()));
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> ASH_DUST = simple("ash_dust", 2, false, 64, "Материал брони тира 1.", "Пригодится на верстаке брони.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> RUSTY_GEAR = simple("rusty_gear", 5, false, 64, "Хлам. Скупщику нравится.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> CHARRED_COIN = simple("charred_coin", 12, false, 64, "Старая монета, оплавленная пеплом.", "Хлам, но ценный.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> ASH_RELIC = simple("ash_relic", 60, true, 16, "Реликвия Пепельной пустоши.", "Очень ценный хлам.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> WEAPON_SHARDS = simple("weapon_shards", 15, false, 64, "Всё, что осталось от оружия после неудачной заточки.", "Скоро: разбор в кузнице.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> STAR_W1 = simple("star_w1", 25, false, 64, "Звезда заточки мира 1.", "Кузница: ПКМ с оружием в руке.", "При неудаче оружие ЛОМАЕТСЯ.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> BLESSED_STAR_W1 = simple("blessed_star_w1", 150, true, 64, "Благословенная звезда мира 1.", "Кузница: Shift+ПКМ с оружием в руке.", "При неудаче заточка падает на 1.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> BRUTE_CRYSTAL = simple("brute_crystal", 40, true, 64, "Кристалл Гнилого Громилы.", "10 шт. — обмен на Тесак Громилы (скоро).");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> BRUTE_TROPHY = simple("brute_trophy", 0, true, 1, "Трофей: Гнилой Громила повержен.", "Для витрины в квартире (скоро).");
    public static final DeferredItem<com.pvpciv.item.BoatSummonItem> OAK_BOAT_T1 = ITEMS.register("oak_boat_t1",
            () -> new com.pvpciv.item.BoatSummonItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<com.pvpciv.item.BoatSummonItem> RIME_BOAT_T2 = ITEMS.register("rime_boat_t2",
            () -> new com.pvpciv.item.BoatSummonItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 2, net.minecraft.world.entity.vehicle.Boat.Type.BIRCH,
                    "Инеевая", "«Скользит так, будто лёд сам её толкает.» — Ледоруб"));

    // --- Тир 1+ (Инеевый Кряж)
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> RIME_BLADE = ITEMS.register("rime_blade",
            () -> new com.pvpciv.item.CivWeaponItem(1, true, com.pvpciv.item.CivWeaponItem.WClass.SWORD, com.pvpciv.loot.Rarity.COMMON, "Клинок изо льда, который не тает. Даже в руках."));
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> FROST_RAPIER = ITEMS.register("frost_rapier",
            () -> new com.pvpciv.item.CivWeaponItem(1, true, com.pvpciv.item.CivWeaponItem.WClass.SABER, com.pvpciv.loot.Rarity.COMMON, "Тонкая, звенящая. Колет быстрее, чем ты моргнёшь."));
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> ICICLE_DAGGER = ITEMS.register("icicle_dagger",
            () -> new com.pvpciv.item.CivWeaponItem(1, true, com.pvpciv.item.CivWeaponItem.WClass.DAGGER, com.pvpciv.loot.Rarity.COMMON, "Сосулька, заточенная с любовью."));
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> GLACIER_AXE = ITEMS.register("glacier_axe",
            () -> new com.pvpciv.item.CivWeaponItem(1, true, com.pvpciv.item.CivWeaponItem.WClass.AXE, com.pvpciv.loot.Rarity.COMMON, "Им рубили лёд на Трассе. Потом — тех, кто мешал."));
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> RIME_SHARD = simple("rime_shard", 4, false, 64, "Материал брони тира 1+.", "Остаётся от инеевых мобов и брони.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> FROZEN_LOCKET = simple("frozen_locket", 45, true, 16, "Медальон, вмёрзший в лёд.", "Внутри чей-то портрет. Ценный хлам.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> LUNA_PLUSH = simple("luna_plush", 120, true, 1, "Плюшевый Пепел. Подозрительно похож на тебя.", "Из шкатулки Луны. Она точно не заметит.");

    private static DeferredItem<com.pvpciv.item.SimpleCivItem> simple(String id, int price, boolean foil, int stack, String... lines) {
        return ITEMS.register(id, () -> new com.pvpciv.item.SimpleCivItem(new Item.Properties().stacksTo(stack), price, foil, lines));
    }

    public static final DeferredItem<BlockItem> APARTMENT_DOOR_ITEM = ITEMS.register("apartment_door",
            () -> new DoubleHighBlockItem(APARTMENT_DOOR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ARENA_CORE_ITEM = ITEMS.registerSimpleBlockItem(ARENA_CORE);

    // --- Entities
    public static final DeferredHolder<EntityType<?>, EntityType<GuardEntity>> GUARD = ENTITIES.register("guard",
            () -> EntityType.Builder.of(GuardEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(10).build("guard"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.TraderEntity>> TRADER = ENTITIES.register("trader",
            () -> EntityType.Builder.of(com.pvpciv.entity.TraderEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(10).build("trader"));
    public static final DeferredHolder<EntityType<?>, EntityType<LunaEntity>> LUNA = ENTITIES.register("luna",
            () -> EntityType.Builder.of(LunaEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(10).build("luna"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.CarmillaEntity>> CARMILLA = ENTITIES.register("carmilla",
            () -> EntityType.Builder.of(com.pvpciv.entity.CarmillaEntity::new, MobCategory.MISC).sized(0.6F, 1.95F).clientTrackingRange(10).build("carmilla"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.EleonoraEntity>> ELEONORA = ENTITIES.register("eleonora",
            () -> EntityType.Builder.of(com.pvpciv.entity.EleonoraEntity::new, MobCategory.MISC).sized(0.6F, 1.95F).clientTrackingRange(10).build("eleonora"));

    // --- Броня
    public static final DeferredRegister<net.minecraft.world.item.ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, PvpCiv.MODID);
    public static final DeferredHolder<net.minecraft.world.item.ArmorMaterial, net.minecraft.world.item.ArmorMaterial> RIME = ARMOR_MATERIALS.register("rime",
            () -> new net.minecraft.world.item.ArmorMaterial(java.util.Map.of(ArmorItem.Type.HELMET, 0, ArmorItem.Type.CHESTPLATE, 0, ArmorItem.Type.LEGGINGS, 0, ArmorItem.Type.BOOTS, 0, ArmorItem.Type.BODY, 0),
                    0, net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_CHAIN, () -> net.minecraft.world.item.crafting.Ingredient.EMPTY,
                    java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(PvpCiv.id("rime"))), 0f, 0f));
    public static final DeferredItem<com.pvpciv.item.CivArmorItem> RIME_HELMET = armor("rime_helmet", ArmorItem.Type.HELMET, "Капюшон со стальным ободом. Уши не мёрзнут.");
    public static final DeferredItem<com.pvpciv.item.CivArmorItem> RIME_CHEST = armor("rime_chestplate", ArmorItem.Type.CHESTPLATE, "Кираса с инеем, который не тает.");
    public static final DeferredItem<com.pvpciv.item.CivArmorItem> RIME_LEGS = armor("rime_leggings", ArmorItem.Type.LEGGINGS, "Поножи на меху. Шуршат на ходу.");
    public static final DeferredItem<com.pvpciv.item.CivArmorItem> RIME_BOOTS = armor("rime_boots", ArmorItem.Type.BOOTS, "Подошва с шипами — для льда и чужих спин.");

    private static DeferredItem<com.pvpciv.item.CivArmorItem> armor(String id, ArmorItem.Type t, String lore) {
        return ITEMS.register(id, () -> new com.pvpciv.item.CivArmorItem(RIME, t, 1.5, "Инеевый комплект", lore));
    }

    // --- Attachments
    public static final Supplier<AttachmentType<StoryData>> STORY = ATTACHMENTS.register("story",
            () -> AttachmentType.serializable(StoryData::new).copyOnDeath().build());

    public static final Supplier<AttachmentType<Integer>> MOB_LEVEL = ATTACHMENTS.register("mob_level",
            () -> AttachmentType.builder(() -> 0).serialize(com.mojang.serialization.Codec.INT).build());
    public static final Supplier<AttachmentType<String>> MOB_KIND = ATTACHMENTS.register("mob_kind",
            () -> AttachmentType.builder(() -> "").serialize(com.mojang.serialization.Codec.STRING).build());

    public static final Supplier<AttachmentType<net.neoforged.neoforge.items.ItemStackHandler>> BAG = ATTACHMENTS.register("bag",
            () -> AttachmentType.serializable(() -> new net.neoforged.neoforge.items.ItemStackHandler(com.pvpciv.inv.Bag.MAX)).copyOnDeath().build());

    // --- Menus
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.pvpciv.menu.CivInventoryMenu>> INV_MENU =
            MENUS.register("inventory", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id, inv, buf) -> new com.pvpciv.menu.CivInventoryMenu(id, inv)));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.pvpciv.menu.ForgeMenu>> FORGE_MENU =
            MENUS.register("forge", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id, inv, buf) -> new com.pvpciv.menu.ForgeMenu(id, inv)));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.pvpciv.menu.DismantleMenu>> DISMANTLE_MENU =
            MENUS.register("dismantle", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id, inv, buf) -> new com.pvpciv.menu.DismantleMenu(id, inv)));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.pvpciv.menu.TraderMenu>> TRADER_MENU =
            MENUS.register("trader", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id, inv, buf) -> new com.pvpciv.menu.TraderMenu(id, inv)));

    // --- Creative tab
    public static final Supplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.literal("PvP Цивилизация"))
            .icon(() -> new ItemStack(KEY_CARD.get()))
            .displayItems((p, out) -> {
                out.accept(KEY_CARD.get());
                for (var it : ITEMS.getEntries()) if (it.get() != KEY_CARD.get()) out.accept(it.get());
            }).build());

    public static void register(IEventBus bus) {
        ModMobs.init();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        ATTACHMENTS.register(bus);
        TABS.register(bus);
        COMPONENTS.register(bus);
        MENUS.register(bus);
        ARMOR_MATERIALS.register(bus);
    }

    public static void onAttributes(EntityAttributeCreationEvent e) {
        e.put(GUARD.get(), StoryNpc.createAttributes().build());
        e.put(LUNA.get(), StoryNpc.createAttributes().build());
        e.put(TRADER.get(), StoryNpc.createAttributes().build());
        e.put(CARMILLA.get(), StoryNpc.createAttributes().build());
        e.put(ELEONORA.get(), StoryNpc.createAttributes().build());
    }
}
