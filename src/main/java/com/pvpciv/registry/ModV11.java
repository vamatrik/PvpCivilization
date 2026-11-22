package com.pvpciv.registry;

import com.pvpciv.block.TrophyShelfBlock;
import com.pvpciv.block.TrophyShelfBlockEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

import static com.pvpciv.registry.ModRegistry.*;

/** 1.1: витрины трофеев по этажам, ритуальная комната, таблички и пульты. */
public final class ModV11 {
    private static DeferredItem<com.pvpciv.item.SimpleCivItem> simple(String id, int price, boolean foil, int stack, String... lines) {
        return ITEMS.register(id, () -> new com.pvpciv.item.SimpleCivItem(new Item.Properties().stacksTo(stack), price, foil, lines));
    }

    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> DEMON_TROPHY = simple("demon_trophy", 0, true, 1, "Трофей: Демоница Пустоты изгнана.", "Для витрины этажа 5 в квартире.");
    public static final DeferredItem<com.pvpciv.item.SimpleCivItem> DARK_SHARD = simple("dark_shard", 900, true, 64, "Осколок темноты.", "Всё, что осталось от оружия после неудачного ритуала.", "Пока — только на продажу.");

    public static final DeferredBlock<TrophyShelfBlock> TROPHY_SHELF = BLOCKS.register("trophy_shelf",
            () -> new TrophyShelfBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion().lightLevel(s -> 9)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrophyShelfBlockEntity>> TROPHY_SHELF_BE =
            BLOCK_ENTITIES.register("trophy_shelf", () -> BlockEntityType.Builder.of(TrophyShelfBlockEntity::new, TROPHY_SHELF.get()).build(null));

    public static final DeferredBlock<com.pvpciv.block.RitualBlock> RITUAL_CORE = BLOCKS.register("ritual_core",
            () -> new com.pvpciv.block.RitualBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion().lightLevel(s -> 10), true));
    public static final DeferredBlock<com.pvpciv.block.RitualBlock> RITUAL_INJECTOR = BLOCKS.register("ritual_injector",
            () -> new com.pvpciv.block.RitualBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion()
                    .lightLevel(s -> s.getValue(com.pvpciv.block.RitualBlock.LIT) ? 11 : 3), false));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pvpciv.block.RitualBlockEntity>> RITUAL_BE =
            BLOCK_ENTITIES.register("ritual", () -> BlockEntityType.Builder.of(com.pvpciv.block.RitualBlockEntity::new, RITUAL_CORE.get(), RITUAL_INJECTOR.get()).build(null));

    public static final DeferredBlock<com.pvpciv.block.CivSignBlock> CIV_SIGN = BLOCKS.register("civ_sign",
            () -> new com.pvpciv.block.CivSignBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DARK_OAK_PLANKS).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion().lightLevel(s -> 4)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.pvpciv.block.CivSignBlockEntity>> SIGN_BE =
            BLOCK_ENTITIES.register("civ_sign", () -> BlockEntityType.Builder.of(com.pvpciv.block.CivSignBlockEntity::new, CIV_SIGN.get()).build(null));

    public static final DeferredBlock<com.pvpciv.block.ConsoleBlock> CONSOLE = BLOCKS.register("console",
            () -> new com.pvpciv.block.ConsoleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion().lightLevel(s -> 8)));

    public static void init() {}
}
