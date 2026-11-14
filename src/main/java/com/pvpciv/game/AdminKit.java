package com.pvpciv.game;

import com.pvpciv.data.StoryData;
import com.pvpciv.inv.Bag;
import com.pvpciv.loot.Gear;
import com.pvpciv.loot.Rarity;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 0.10.3 — /civ admin full: снимок аккаунта (прогресс, сумка, инвентарь) и «полный» персонаж для теста последнего этажа;
 * /civ admin back — вернуть всё как было по снимку.
 */
public final class AdminKit {
    private AdminKit() {}
    private static final String KEY = "pvpciv_admin_backup";

    public static int full(ServerPlayer p, CommandSourceStack src) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(KEY)) {   // снимок — только первый раз, чтобы повторный full не затёр настоящий аккаунт
            CompoundTag b = new CompoundTag();
            b.put("story", Progress.d(p).serializeNBT(p.registryAccess()).copy());   // глубокая копия: иначе full правил врата прямо в снимке
            b.put("bag", Bag.get(p).serializeNBT(p.registryAccess()).copy());
            b.put("inv", p.getInventory().save(new ListTag()));
            root.put(KEY, b);
        }
        StoryData d = Progress.d(p);
        d.liftOk = true;
        d.trophies = 63; d.showcase = 63;
        d.level = Progress.levelCap(d); d.xp = 0;
        d.money = Math.max(d.money, 200_000);
        d.homeGate = 3;
        for (Gates.Gate g : Gates.ALL) d.gates.putInt(g.id(), 2);
        d.libQuest = Math.max(d.libQuest, 2);
        d.extraSlots = Bag.MAX;
        d.touchSkill = true;   // 0.10.5: второй навык (Юки)
        p.setData(ModRegistry.STORY.get(), d);
        // снаряжение этажа 3: легендарное +10
        put(p, EquipmentSlot.HEAD, ModRegistry.SAMURAI_HELMET.get());
        put(p, EquipmentSlot.CHEST, ModRegistry.SAMURAI_CHEST.get());
        put(p, EquipmentSlot.LEGS, ModRegistry.SAMURAI_LEGS.get());
        put(p, EquipmentSlot.FEET, ModRegistry.SAMURAI_BOOTS.get());
        give(p, ModRegistry.SAMURAI_KATANA.get());
        give(p, ModRegistry.FOXFIRE_BLADE.get());
        give(p, ModRegistry.KUNAI.get()); give(p, ModRegistry.KUNAI.get());
        give(p, ModRegistry.TETSUBO.get());
        Bag.give(p, new ItemStack(ModRegistry.ALMANAC.get()));
        Bag.give(p, new ItemStack(ModRegistry.STAR_W1.get(), 16));
        Bag.give(p, new ItemStack(ModRegistry.BLESSED_STAR_W1.get(), 8));
        Progress.applyStats(p);
        p.setHealth(p.getMaxHealth());
        Story.sync(p);
        Gates.sync(p);
        src.sendSuccess(() -> Component.literal("Аккаунт «полный»: ур. " + d.level + ", все трофеи и врата, снаряжение этажа 3 (+10). Вернуть — /civ admin back").withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    public static int back(ServerPlayer p, CommandSourceStack src) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(KEY)) { src.sendFailure(Component.literal("Снимка нет — /civ admin full ещё не делали.")); return 0; }
        CompoundTag b = root.getCompound(KEY);
        StoryData d = new StoryData();
        d.deserializeNBT(p.registryAccess(), b.getCompound("story").copy());
        p.setData(ModRegistry.STORY.get(), d);
        var bag = Bag.get(p);
        bag.deserializeNBT(p.registryAccess(), b.getCompound("bag"));
        p.getInventory().clearContent();
        p.getInventory().load(b.getList("inv", 10));
        root.remove(KEY);
        Progress.applyStats(p);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        Story.sync(p);
        Gates.sync(p);
        src.sendSuccess(() -> Component.literal("Аккаунт возвращён как был.").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    /** Сбросить починку всех врат (найденные остаются найденными) и титулы за врата — для тех, кому back 0.10.3/0.10.4 оставил врата открытыми. */
    public static int resetGates(ServerPlayer p, CommandSourceStack src) {
        StoryData d = Progress.d(p);
        for (String k : d.gates.getAllKeys()) if (k.startsWith("g_") && d.gates.getInt(k) >= 2) d.gates.putInt(k, 1);
        d.titles.remove("gates_1"); d.titles.remove("gates_2"); d.titles.remove("gates_3");
        if (d.activeTitle.startsWith("gates_")) d.activeTitle = "";
        p.setData(ModRegistry.STORY.get(), d);
        Progress.applyStats(p);
        Story.sync(p); Gates.sync(p);
        src.sendSuccess(() -> Component.literal("Починка врат сброшена.").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static ItemStack top(Item it) {
        ItemStack st = new ItemStack(it);
        Gear.withRarity(st, Rarity.LEGENDARY);
        Gear.setStars(st, 10);
        return st;
    }
    private static void put(ServerPlayer p, EquipmentSlot s, Item it) {
        ItemStack old = p.getItemBySlot(s);
        if (!old.isEmpty()) Bag.give(p, old.copy());
        p.setItemSlot(s, top(it));
    }
    /** 0.11: «полный» аккаунт этажа 4 — уровень 27, экзокостюм, оружие тира 4, лазуратрон, модули, все квесты стола. */
    public static int full4(ServerPlayer p, CommandSourceStack src) {
        full(p, src);
        StoryData d = Progress.d(p);
        d.trophies = 127; d.showcase = 127;
        d.level = Progress.levelCap(d); d.xp = 0;
        d.gates.putInt("ench_q", 4);
        d.gates.putInt("stella_q", 1);
        p.setData(ModRegistry.STORY.get(), d);
        put(p, EquipmentSlot.HEAD, com.pvpciv.registry.ModF4.EXO_HELMET.get());
        put(p, EquipmentSlot.CHEST, com.pvpciv.registry.ModF4.EXO_CHEST.get());
        put(p, EquipmentSlot.LEGS, com.pvpciv.registry.ModF4.EXO_LEGS.get());
        put(p, EquipmentSlot.FEET, com.pvpciv.registry.ModF4.EXO_BOOTS.get());
        give(p, com.pvpciv.registry.ModF4.PLASMA_CUTTER.get());
        give(p, com.pvpciv.registry.ModF4.ION_DAGGER.get()); give(p, com.pvpciv.registry.ModF4.ION_DAGGER.get());
        give(p, com.pvpciv.registry.ModF4.GRAVITY_HAMMER.get());
        give(p, com.pvpciv.registry.ModF4.VINE_AXE.get());
        Bag.give(p, new ItemStack(com.pvpciv.registry.ModF4.LAZURATRON.get(), 99)); Bag.give(p, new ItemStack(com.pvpciv.registry.ModF4.LAZURATRON.get(), 99));
        Bag.give(p, new ItemStack(com.pvpciv.registry.ModF4.MODULE_BASIC.get(), 2));
        Bag.give(p, new ItemStack(com.pvpciv.registry.ModF4.MODULE_IMPROVED.get(), 1));
        Bag.give(p, new ItemStack(com.pvpciv.registry.ModF4.CAT_FOCUS.get(), 3));
        Bag.give(p, new ItemStack(com.pvpciv.registry.ModF4.CAT_ANCHOR.get(), 2));
        Bag.give(p, new ItemStack(com.pvpciv.registry.ModF4.CAT_RESONATOR.get(), 2));
        Bag.give(p, new ItemStack(com.pvpciv.registry.ModF4.PHASE_ANCHOR.get()));
        Progress.applyStats(p);
        p.setHealth(p.getMaxHealth());
        Story.sync(p);
        src.sendSuccess(() -> Component.literal("Аккаунт этажа 4: ур. " + Progress.d(p).level + ", экзокостюм, оружие т4, стол открыт. Вернуть — /civ admin back").withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static void give(ServerPlayer p, Item it) { Bag.give(p, top(it)); }
}
