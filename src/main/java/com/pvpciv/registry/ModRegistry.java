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
    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, PvpCiv.MODID);
    public static final DeferredHolder<net.minecraft.world.effect.MobEffect, net.minecraft.world.effect.MobEffect> STUN = MOB_EFFECTS.register("stun", com.pvpciv.combat.StunEffect::new);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, PvpCiv.MODID);

    // --- Data components
    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<Integer>> RARITY =
            COMPONENTS.registerComponentType("rarity", b -> b.persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<Integer>> STARS =
            COMPONENTS.registerComponentType("stars", b -> b.persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<Integer>> ARMOR_XP =
            COMPONENTS.registerComponentType("armor_xp", b -> b.persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
    /** 0.8.6: уровень ядра брони (+1…+10) — до какого улучшения подходит. */
    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<Integer>> CORE_LEVEL =
            COMPONENTS.registerComponentType("core_level", b -> b.persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<net.minecraft.core.component.DataComponentType<?>, net.minecraft.core.component.DataComponentType<Integer>> CORE_POWER =
            COMPONENTS.registerComponentType("core_power", b -> b.persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    // --- Blocks
    public static final DeferredBlock<ApartmentDoorBlock> APARTMENT_DOOR = BLOCKS.register("apartment_door",
            () -> new ApartmentDoorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(-1.0F, 3600000.0F).noLootTable()));
    public static final DeferredBlock<ArenaCoreBlock> ARENA_CORE = BLOCKS.register("arena_core",
            () -> new ArenaCoreBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LODESTONE).strength(-1.0F, 3600000.0F)
                    .lightLevel(s -> 13).sound(SoundType.LODESTONE).noLootTable().noOcclusion()));

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
            () -> new com.pvpciv.block.StashBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion()));
    public static final DeferredBlock<com.pvpciv.block.DismantleBlock> DISMANTLE = BLOCKS.register("dismantle_table",
            () -> new com.pvpciv.block.DismantleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FLETCHING_TABLE).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion()));
    public static final DeferredItem<BlockItem> DISMANTLE_ITEM = ITEMS.registerSimpleBlockItem(DISMANTLE);
    // 0.7: алтарь инфузии (подвал), пьедесталы, стол брони и домашний сундук (в квартирах)
    public static final DeferredBlock<com.pvpciv.block.CivStationBlock> INFUSION_ALTAR = BLOCKS.register("infusion_altar",
            () -> new com.pvpciv.block.CivStationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE).strength(-1.0F, 3600000.0F).lightLevel(s -> 7).noLootTable().noOcclusion(), false, com.pvpciv.block.CivStationBlock.Kind.ALTAR));
    public static final DeferredItem<BlockItem> INFUSION_ALTAR_ITEM = ITEMS.registerSimpleBlockItem(INFUSION_ALTAR);
    public static final DeferredBlock<com.pvpciv.block.CivStationBlock> INFUSION_PEDESTAL = BLOCKS.register("infusion_pedestal",
            () -> new com.pvpciv.block.CivStationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE).strength(-1.0F, 3600000.0F).lightLevel(s -> 5).noLootTable().noOcclusion(), false, com.pvpciv.block.CivStationBlock.Kind.PEDESTAL));
    public static final DeferredItem<BlockItem> INFUSION_PEDESTAL_ITEM = ITEMS.registerSimpleBlockItem(INFUSION_PEDESTAL);
    public static final DeferredBlock<com.pvpciv.block.CivStationBlock> ARMOR_BENCH = BLOCKS.register("armor_bench",
            () -> new com.pvpciv.block.CivStationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion(), true, com.pvpciv.block.CivStationBlock.Kind.ARMOR));
    public static final DeferredItem<BlockItem> ARMOR_BENCH_ITEM = ITEMS.registerSimpleBlockItem(ARMOR_BENCH);
    public static final DeferredBlock<com.pvpciv.block.CivStationBlock> HOME_CHEST = BLOCKS.register("home_chest",
            () -> new com.pvpciv.block.CivStationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion(), true, com.pvpciv.block.CivStationBlock.Kind.HOME_CHEST));
    public static final DeferredItem<BlockItem> HOME_CHEST_ITEM = ITEMS.registerSimpleBlockItem(HOME_CHEST);
    public static final DeferredBlock<com.pvpciv.block.CivStationBlock> TROPHY_CASE = BLOCKS.register("trophy_case",
            () -> new com.pvpciv.block.CivStationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion().lightLevel(s -> 6), true, com.pvpciv.block.CivStationBlock.Kind.SHOWCASE));
    public static final DeferredItem<BlockItem> TROPHY_CASE_ITEM = ITEMS.registerSimpleBlockItem(TROPHY_CASE);
    /** Дороги Неонового города: скользят как лёд (лодки едут по городу). */
    public static final DeferredBlock<Block> NEON_ROAD = BLOCKS.register("neon_road",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BLACK_CONCRETE).friction(0.98f).strength(-1.0F, 3600000.0F).noLootTable()));
    public static final DeferredItem<BlockItem> NEON_ROAD_ITEM = ITEMS.registerSimpleBlockItem(NEON_ROAD);
    public static final DeferredBlock<Block> NEON_ROAD_LINE = BLOCKS.register("neon_road_line",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.BLACK_CONCRETE).friction(0.98f).strength(-1.0F, 3600000.0F).noLootTable()));
    public static final DeferredItem<BlockItem> NEON_ROAD_LINE_ITEM = ITEMS.registerSimpleBlockItem(NEON_ROAD_LINE);
    public static final DeferredBlock<com.pvpciv.block.CivStationBlock> LIFT_PANEL = BLOCKS.register("lift_panel",
            () -> new com.pvpciv.block.CivStationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion().lightLevel(s -> 12), true, com.pvpciv.block.CivStationBlock.Kind.LIFT));
    public static final DeferredItem<BlockItem> LIFT_PANEL_ITEM = ITEMS.registerSimpleBlockItem(LIFT_PANEL);
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
            () -> new com.pvpciv.item.CivWeaponItem(1, com.pvpciv.item.CivWeaponItem.WClass.HAMMER, com.pvpciv.loot.Rarity.EPIC, "Фирменное оружие Гнилого Громилы. Пахнет соответственно.").skill(com.pvpciv.game.WeaponSkills.Skill.QUAKE));

    // --- Материалы, хлам, звёзды
    public static final DeferredItem<com.pvpciv.item.SparkItem> SPARKS = ITEMS.register("sparks", () -> new com.pvpciv.item.SparkItem(new Item.Properties()));
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> ASH_DUST = simple("ash_dust", 2, false, 64, "Материал брони тира 1.", "Пригодится на верстаке брони.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> RUSTY_GEAR = simple("rusty_gear", 5, false, 64, "Хлам. Скупщику нравится.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> CHARRED_COIN = simple("charred_coin", 12, false, 64, "Старая монета, оплавленная пеплом.", "Хлам, но ценный.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> ASH_RELIC = simple("ash_relic", 60, true, 16, "Реликвия Пепельной пустоши.", "Очень ценный хлам.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> WEAPON_SHARDS = simple("weapon_shards", 15, false, 64, "Всё, что осталось от оружия после неудачной заточки.", "Скоро: разбор в кузнице.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> STAR_W1 = simple("star_w1", 25, false, 64, "Звезда заточки мира 1.", "Наковальня в квартире: оружие + звезда.", "Неудача — оружие ЛОМАЕТСЯ. Алтарь: 8 звёзд → ✦.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> BLESSED_STAR_W1 = simple("blessed_star_w1", 150, true, 64, "Благословенная звезда мира 1.", "Наковальня: неудача — заточка −1, оружие цело.", "Алтарь инфузии: из оружия +5 и 8 звёзд.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> BRUTE_CRYSTAL = simple("brute_crystal", 40, true, 64, "Кристалл Гнилого Громилы.", "10 шт. — обмен на Тесак Громилы (скоро).");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> BRUTE_TROPHY = simple("brute_trophy", 0, true, 1, "Трофей: Гнилой Громила повержен.", "ПКМ по витрине в квартире — выставить.");
    public static final DeferredItem<com.pvpciv.item.BoatSummonItem> OAK_BOAT_T1 = ITEMS.register("oak_boat_t1",
            () -> new com.pvpciv.item.BoatSummonItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<com.pvpciv.item.BoatSummonItem> NEON_TAXI_T3 = ITEMS.register("neon_taxi_t3",
            () -> new com.pvpciv.item.BoatSummonItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 3, net.minecraft.world.entity.vehicle.Boat.Type.OAK, "Неоновое такси",
                    "«Шашечки не врут: по неоновому асфальту — как по льду.»"));
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
    public static final DeferredItem<Item> BLOOD_STAR_W1 = ITEMS.register("blood_star_w1", () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)) {
        @Override public void appendHoverText(ItemStack st, TooltipContext c, java.util.List<net.minecraft.network.chat.Component> tip, net.minecraft.world.item.TooltipFlag f) {
            tip.add(net.minecraft.network.chat.Component.literal("Звезда заточки • Мир 1").withStyle(net.minecraft.ChatFormatting.DARK_RED));
            tip.add(net.minecraft.network.chat.Component.literal("Шанс как у обычной. При провале оружие не страдает —").withStyle(net.minecraft.ChatFormatting.GRAY));
            tip.add(net.minecraft.network.chat.Component.literal("но Кармилла забирает тебя в подвал на 10 минут.").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        @Override public boolean isFoil(ItemStack st) { return true; }
    });
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> KEEPER_SPEAR = ITEMS.register("keeper_spear",
            () -> new com.pvpciv.item.CivWeaponItem(1, true, com.pvpciv.item.CivWeaponItem.WClass.SABER, com.pvpciv.loot.Rarity.EPIC, "Копьё Хранительницы Склепа. Холодное даже сквозь перчатку.").skill(com.pvpciv.game.WeaponSkills.Skill.FROST_LUNGE));
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> KEEPER_CRYSTAL = simple("keeper_crystal", 70, true, 64, "Осколок короны Хранительницы Склепа.", "Редкий материал Кряжа: ядро брони и инфузия.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> KEEPER_TROPHY = simple("keeper_trophy", 0, true, 1, "Трофей: Хранительница Склепа повержена.", "ПКМ по витрине в квартире — выставить.");
    public static final DeferredItem<com.pvpciv.item.ArmorCoreItem> ARMOR_CORE = ITEMS.register("armor_core", () -> new com.pvpciv.item.ArmorCoreItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> TEMPER_SCROLL = simple("temper_scroll", 50, true, 16, "Свиток закалки.", "Стол брони: нужен для улучшения до +5 и до +10.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> RIME_SHARD = simple("rime_shard", 4, false, 64, "Материал брони тира 1+.", "Остаётся от инеевых мобов и брони.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> FROZEN_LOCKET = simple("frozen_locket", 45, true, 16, "Медальон, вмёрзший в лёд.", "Сердце ядра брони: алтарь инфузии в подвале.");
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

    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.LunaGuideEntity>> LUNA_GUIDE = ENTITIES.register("luna_guide",
            () -> EntityType.Builder.of(com.pvpciv.entity.LunaGuideEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(10).build("luna_guide"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.CityNpcEntity>> TIMOFEY = ENTITIES.register("timofey",
            () -> EntityType.Builder.<com.pvpciv.entity.CityNpcEntity>of((t, l) -> new com.pvpciv.entity.CityNpcEntity(t, l, com.pvpciv.entity.CityNpcEntity.Role.TIMOFEY), MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(10).build("timofey"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.CityNpcEntity>> PARACHUTIST = ENTITIES.register("parachutist",
            () -> EntityType.Builder.<com.pvpciv.entity.CityNpcEntity>of((t, l) -> new com.pvpciv.entity.CityNpcEntity(t, l, com.pvpciv.entity.CityNpcEntity.Role.PARACHUTIST), MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(10).build("parachutist"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.CityNpcEntity>> PLUMBER = ENTITIES.register("plumber",
            () -> EntityType.Builder.<com.pvpciv.entity.CityNpcEntity>of((t, l) -> new com.pvpciv.entity.CityNpcEntity(t, l, com.pvpciv.entity.CityNpcEntity.Role.PLUMBER), MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(10).build("plumber"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.CityNpcEntity>> SMUGGLER = ENTITIES.register("smuggler",
            () -> EntityType.Builder.<com.pvpciv.entity.CityNpcEntity>of((t, l) -> new com.pvpciv.entity.CityNpcEntity(t, l, com.pvpciv.entity.CityNpcEntity.Role.SMUGGLER), MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(10).build("smuggler"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.LiftCabin>> LIFT_CABIN = ENTITIES.register("lift_cabin",
            () -> EntityType.Builder.<com.pvpciv.entity.LiftCabin>of(com.pvpciv.entity.LiftCabin::new, MobCategory.MISC).sized(2.6F, 3.0F).clientTrackingRange(16).updateInterval(1).build("lift_cabin"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.GateEntity>> GATE = ENTITIES.register("gate",
            () -> EntityType.Builder.<com.pvpciv.entity.GateEntity>of(com.pvpciv.entity.GateEntity::new, MobCategory.MISC).sized(1.4F, 2.2F).clientTrackingRange(10).build("gate"));
    public static final DeferredItem<Item> MONORAIL_SEGMENT = ITEMS.register("monorail_segment", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.CivBoat>> CIV_BOAT = ENTITIES.register("civ_boat",
            () -> EntityType.Builder.<com.pvpciv.entity.CivBoat>of(com.pvpciv.entity.CivBoat::new, MobCategory.MISC).sized(1.375F, 0.5625F).clientTrackingRange(10).build("civ_boat"));
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

    // --- Этаж 2: броня ликвидатора (канализация, тир 2+)
    public static final DeferredHolder<net.minecraft.world.item.ArmorMaterial, net.minecraft.world.item.ArmorMaterial> HAZMAT = ARMOR_MATERIALS.register("hazmat",
            () -> new net.minecraft.world.item.ArmorMaterial(java.util.Map.of(ArmorItem.Type.HELMET, 0, ArmorItem.Type.CHESTPLATE, 0, ArmorItem.Type.LEGGINGS, 0, ArmorItem.Type.BOOTS, 0, ArmorItem.Type.BODY, 0),
                    0, net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER, () -> net.minecraft.world.item.crafting.Ingredient.EMPTY,
                    java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(PvpCiv.id("hazmat"))), 0f, 0f));
    private static DeferredItem<com.pvpciv.item.CivArmorItem> armor2(String id, ArmorItem.Type t, String lore) {
        return ITEMS.register(id, () -> new com.pvpciv.item.CivArmorItem(HAZMAT, t, 2.5, "Комплект ликвидатора", lore));
    }
    public static final DeferredItem<com.pvpciv.item.CivArmorItem> HAZMAT_HELMET = armor2("hazmat_helmet", ArmorItem.Type.HELMET, "Противогаз. Запах канализации — всё равно чувствуется.");
    public static final DeferredItem<com.pvpciv.item.CivArmorItem> HAZMAT_CHEST = armor2("hazmat_chestplate", ArmorItem.Type.CHESTPLATE, "Прорезиненный костюм с бронепластиной.");
    public static final DeferredItem<com.pvpciv.item.CivArmorItem> HAZMAT_LEGS = armor2("hazmat_leggings", ArmorItem.Type.LEGGINGS, "Штаны-забродни. Крысы не прокусят. Почти.");
    public static final DeferredItem<com.pvpciv.item.CivArmorItem> HAZMAT_BOOTS = armor2("hazmat_boots", ArmorItem.Type.BOOTS, "Сапоги с подковкой. Хлюпают.");

    // --- Этаж 2: оружие тира 2 (улица) и 2+ (канализация), навыковое оружие боссов
    private static DeferredItem<com.pvpciv.item.CivWeaponItem> w2(String id, boolean plus, com.pvpciv.item.CivWeaponItem.WClass c, String lore) {
        return ITEMS.register(id, () -> new com.pvpciv.item.CivWeaponItem(2, plus, c, com.pvpciv.loot.Rarity.COMMON, lore));
    }
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> NAIL_BAT = w2("nail_bat", false, com.pvpciv.item.CivWeaponItem.WClass.HAMMER, "Бита с гвоздями. Аргумент в любом споре.");
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> BRASS_KNUCKLES = w2("brass_knuckles", false, com.pvpciv.item.CivWeaponItem.WClass.DAGGER, "Кастет. Быстро, близко, больно.");
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> MACHETE = w2("machete", false, com.pvpciv.item.CivWeaponItem.WClass.SABER, "Мачете. Им раньше рубили кусты в парке.");
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> FIRE_AXE = w2("fire_axe", false, com.pvpciv.item.CivWeaponItem.WClass.AXE, "Пожарный топор. Разбить стекло — и не только.");
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> STREET_BLADE = w2("street_blade", false, com.pvpciv.item.CivWeaponItem.WClass.SWORD, "Клинок из рессоры. Самодельный, но честный.");
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> PIPE_WRENCH = w2("pipe_wrench", true, com.pvpciv.item.CivWeaponItem.WClass.HAMMER, "Газовый ключ сантехника. Откручивает всё, включая головы.");
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> SEWER_SHIV = w2("sewer_shiv", true, com.pvpciv.item.CivWeaponItem.WClass.DAGGER, "Заточка из трубы. Пахнет так же.");
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> REBAR_SWORD = w2("rebar_sword", true, com.pvpciv.item.CivWeaponItem.WClass.SWORD, "Арматура, заточенная до блеска. Почти меч.");
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> TOXIC_HATCHET = w2("toxic_hatchet", true, com.pvpciv.item.CivWeaponItem.WClass.AXE, "Топорик в зелёной слизи. Не облизывать.");
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> POLICE_BATON = ITEMS.register("police_baton",
            () -> new com.pvpciv.item.CivWeaponItem(2, false, com.pvpciv.item.CivWeaponItem.WClass.SWORD, com.pvpciv.loot.Rarity.EPIC, "Дубинка, которую Дон отнял у последнего честного копа.").skill(com.pvpciv.game.WeaponSkills.Skill.STUN_STRIKE));
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> RAT_PIPE = ITEMS.register("rat_pipe",
            () -> new com.pvpciv.item.CivWeaponItem(2, true, com.pvpciv.item.CivWeaponItem.WClass.SABER, com.pvpciv.loot.Rarity.EPIC, "Дудочка Крысиного короля. Крысы слушаются. Иногда.").skill(com.pvpciv.game.WeaponSkills.Skill.RAT_CALL));
    // материалы, трофеи, парашют
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> NEON_TUBE = simple("neon_tube", 6, false, 64, "Неоновая трубка.", "Хлам улиц этажа 2.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> GOLD_CHAIN = simple("gold_chain", 70, true, 16, "Золотая цепь гопника.", "Ценный хлам.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> GAS_MASK_FILTER = simple("gas_filter", 50, true, 16, "Фильтр противогаза.", "Сердце ядра брони ликвидатора (алтарь).");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> RUBBER_SCRAP = simple("rubber_scrap", 5, false, 64, "Обрезки резины.", "Материал брони тира 2+.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> DON_TROPHY = simple("don_trophy", 0, true, 1, "Трофей: Дон Витторио повержен.", "Для витрины в квартире.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> RAT_KING_TROPHY = simple("rat_king_trophy", 0, true, 1, "Трофей: Крысиный король повержен.", "Для витрины в квартире.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> DON_RING = simple("don_ring", 80, true, 64, "Перстень Дона.", "Редкий материал этажа 2.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> RAT_CROWN_SHARD = simple("rat_crown_shard", 90, true, 64, "Обломок крысиной короны.", "Редкий материал этажа 2.");
    public static final DeferredItem<com.pvpciv.item.BagBuckleItem> SMUGGLER_PACK = ITEMS.register("smuggler_pack",
            () -> new com.pvpciv.item.BagBuckleItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 4));
    public static final DeferredItem<com.pvpciv.item.CivWeaponItem> SAMURAI_KATANA = ITEMS.register("samurai_katana",
            () -> new com.pvpciv.item.CivWeaponItem(3, false, com.pvpciv.item.CivWeaponItem.WClass.SWORD, com.pvpciv.loot.Rarity.RARE, "Контрабанда с этажа 3. Сталь, сложенная тысячу раз. Говорят."));
    public static final DeferredItem<Item> PHONE = ITEMS.register("phone", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)) {
        @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level l, net.minecraft.world.entity.player.Player p, net.minecraft.world.InteractionHand h) {
            if (p instanceof net.minecraft.server.level.ServerPlayer sp) com.pvpciv.game.Phone.open(sp);
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(p.getItemInHand(h), l.isClientSide);
        }
        @Override public void appendHoverText(ItemStack st, TooltipContext c, java.util.List<net.minecraft.network.chat.Component> tip, net.minecraft.world.item.TooltipFlag f) {
            tip.add(net.minecraft.network.chat.Component.literal("Подарок Элеоноры. Экран в трещинах, но работает.").withStyle(net.minecraft.ChatFormatting.GRAY));
            tip.add(net.minecraft.network.chat.Component.literal("ПКМ — выкуп из темницы, доставка еды, такси.").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
        }
    });
    /** 3D-купол парашюта (только для отображения над игроком). */
    public static final DeferredItem<Item> PARACHUTE_CANOPY = ITEMS.register("parachute_canopy", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PARACHUTE = ITEMS.register("parachute", () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)) {
        @Override public void appendHoverText(ItemStack st, TooltipContext c, java.util.List<net.minecraft.network.chat.Component> tip, net.minecraft.world.item.TooltipFlag f) {
            tip.add(net.minecraft.network.chat.Component.literal("Раскрывается сам, если долго падаешь.").withStyle(net.minecraft.ChatFormatting.GRAY));
            tip.add(net.minecraft.network.chat.Component.literal("Упал с этажа — плавно опустишься на этаж ниже.").withStyle(net.minecraft.ChatFormatting.GRAY));
            tip.add(net.minecraft.network.chat.Component.literal("Работает из сумки. Не тратится.").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
        }
    });

    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PvpCiv.MODID);
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, net.minecraft.world.level.block.entity.BlockEntityType<com.pvpciv.block.StashBlockEntity>> STASH_BE =
            BLOCK_ENTITIES.register("stash", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(com.pvpciv.block.StashBlockEntity::new, STASH.get()).build(null));
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, net.minecraft.world.level.block.entity.BlockEntityType<com.pvpciv.block.PedestalBlockEntity>> PEDESTAL_BE =
            BLOCK_ENTITIES.register("pedestal", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(com.pvpciv.block.PedestalBlockEntity::new, INFUSION_ALTAR.get(), INFUSION_PEDESTAL.get()).build(null));
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, net.minecraft.world.level.block.entity.BlockEntityType<com.pvpciv.block.ShowcaseBlockEntity>> SHOWCASE_BE =
            BLOCK_ENTITIES.register("showcase", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(com.pvpciv.block.ShowcaseBlockEntity::new, TROPHY_CASE.get()).build(null));

    // --- Attachments
    public static final Supplier<AttachmentType<StoryData>> STORY = ATTACHMENTS.register("story",
            () -> AttachmentType.serializable(StoryData::new).copyOnDeath().build());

    public static final Supplier<AttachmentType<Integer>> MOB_LEVEL = ATTACHMENTS.register("mob_level",
            () -> AttachmentType.builder(() -> 0).serialize(com.mojang.serialization.Codec.INT).build());
    public static final Supplier<AttachmentType<String>> MOB_KIND = ATTACHMENTS.register("mob_kind",
            () -> AttachmentType.builder(() -> "").serialize(com.mojang.serialization.Codec.STRING).build());

    public static final Supplier<AttachmentType<net.neoforged.neoforge.items.ItemStackHandler>> BAG = ATTACHMENTS.register("bag",
            () -> AttachmentType.serializable(() -> new net.neoforged.neoforge.items.ItemStackHandler(com.pvpciv.inv.Bag.MAX)).copyOnDeath().build());
    /** Домашний сундук: 45 слотов, у каждого игрока свой (как эндер-сундук). */
    public static final Supplier<AttachmentType<net.neoforged.neoforge.items.ItemStackHandler>> HOME_STORAGE = ATTACHMENTS.register("home_storage",
            () -> AttachmentType.serializable(() -> new net.neoforged.neoforge.items.ItemStackHandler(com.pvpciv.game.HomeChest.SIZE)).copyOnDeath().build());

    // --- Menus
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.pvpciv.menu.CivInventoryMenu>> INV_MENU =
            MENUS.register("inventory", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id, inv, buf) -> new com.pvpciv.menu.CivInventoryMenu(id, inv)));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.pvpciv.menu.ForgeMenu>> FORGE_MENU =
            MENUS.register("forge", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id, inv, buf) -> new com.pvpciv.menu.ForgeMenu(id, inv)));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.pvpciv.menu.DismantleMenu>> DISMANTLE_MENU =
            MENUS.register("dismantle", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id, inv, buf) -> new com.pvpciv.menu.DismantleMenu(id, inv)));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.pvpciv.menu.TraderMenu>> TRADER_MENU =
            MENUS.register("trader", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id, inv, buf) -> new com.pvpciv.menu.TraderMenu(id, inv)));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.pvpciv.menu.InfusionMenu>> INFUSION_MENU =
            MENUS.register("infusion", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id, inv, buf) -> new com.pvpciv.menu.InfusionMenu(id, inv, buf.readBlockPos())));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.pvpciv.menu.ArmorBenchMenu>> ARMOR_MENU =
            MENUS.register("armor_bench", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id, inv, buf) -> new com.pvpciv.menu.ArmorBenchMenu(id, inv)));
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<com.pvpciv.menu.HomeChestMenu>> HOME_CHEST_MENU =
            MENUS.register("home_chest", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id, inv, buf) -> new com.pvpciv.menu.HomeChestMenu(id, inv)));

    // --- Creative tab
    public static final Supplier<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.literal("PvP Цивилизация"))
            .icon(() -> new ItemStack(KEY_CARD.get()))
            .displayItems((p, out) -> {
                out.accept(KEY_CARD.get());
                for (var it : ITEMS.getEntries()) if (it.get() != KEY_CARD.get()) out.accept(it.get());
            }).build());

    // ------------------------------------------------------------------ 0.8.4: звуки, курьер, зелья Элеоноры
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, PvpCiv.MODID);
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> PHONE_RING = SOUNDS.register("phone_ring",
            () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(PvpCiv.id("phone_ring")));
    public static final DeferredHolder<EntityType<?>, EntityType<com.pvpciv.entity.CityNpcEntity>> COURIER = ENTITIES.register("courier",
            () -> EntityType.Builder.<com.pvpciv.entity.CityNpcEntity>of((t, l) -> new com.pvpciv.entity.CityNpcEntity(t, l, com.pvpciv.entity.CityNpcEntity.Role.COURIER), MobCategory.MISC).sized(0.6F, 1.8F).clientTrackingRange(10).build("courier"));
    public static final DeferredItem<com.pvpciv.item.CivPotionItem> POTION_HEAL = potion("potion_heal", com.pvpciv.item.CivPotionItem.Kind.HEAL);
    public static final DeferredItem<com.pvpciv.item.CivPotionItem> POTION_REGEN = potion("potion_regen", com.pvpciv.item.CivPotionItem.Kind.REGEN);
    public static final DeferredItem<com.pvpciv.item.CivPotionItem> POTION_SWIFT = potion("potion_swift", com.pvpciv.item.CivPotionItem.Kind.SWIFT);
    public static final DeferredItem<com.pvpciv.item.CivPotionItem> POTION_MIGHT = potion("potion_might", com.pvpciv.item.CivPotionItem.Kind.MIGHT);
    public static final DeferredItem<com.pvpciv.item.CivPotionItem> POTION_STONE = potion("potion_stone", com.pvpciv.item.CivPotionItem.Kind.STONE);

    private static DeferredItem<com.pvpciv.item.CivPotionItem> potion(String id, com.pvpciv.item.CivPotionItem.Kind k) {
        return ITEMS.register(id, () -> new com.pvpciv.item.CivPotionItem(new Item.Properties().stacksTo(16), k));
    }

    public static void register(IEventBus bus) {
        ModMobs.init();
        SOUNDS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        ATTACHMENTS.register(bus);
        TABS.register(bus);
        COMPONENTS.register(bus);
        MENUS.register(bus);
        ARMOR_MATERIALS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MOB_EFFECTS.register(bus);
    }

    public static void onAttributes(EntityAttributeCreationEvent e) {
        e.put(GUARD.get(), StoryNpc.createAttributes().build());
        e.put(LUNA.get(), StoryNpc.createAttributes().build());
        e.put(LUNA_GUIDE.get(), StoryNpc.createAttributes().build());
        e.put(TIMOFEY.get(), StoryNpc.createAttributes().build());
        e.put(PARACHUTIST.get(), StoryNpc.createAttributes().build());
        e.put(PLUMBER.get(), StoryNpc.createAttributes().build());
        e.put(COURIER.get(), StoryNpc.createAttributes().build());
        e.put(SMUGGLER.get(), StoryNpc.createAttributes().build());
        e.put(TRADER.get(), StoryNpc.createAttributes().build());
        e.put(CARMILLA.get(), StoryNpc.createAttributes().build());
        e.put(ELEONORA.get(), StoryNpc.createAttributes().build());
    }
}
