package com.pvpciv.loot;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.pvpciv.item.CivWeaponItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

/**
 * Лут-таблицы цивилизации: data/<ns>/civ_loot/<name>.json.
 * Каждая запись роллится независимо: {"item": id, "chance": 0.5, "min": 1, "max": 3}
 * или {"weapons": [ids], "chance": 0.05, "min_rarity": "RARE", "mythic": false}.
 */
public class LootTables extends SimpleJsonResourceReloadListener {
    public static final LootTables INSTANCE = new LootTables();
    private static final Map<String, JsonArray> TABLES = new HashMap<>();

    private LootTables() { super(new Gson(), "civ_loot"); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager rm, ProfilerFiller pf) {
        TABLES.clear();
        map.forEach((id, json) -> {
            try {
                TABLES.put(id.getPath(), json.getAsJsonObject().getAsJsonArray("entries"));
            } catch (Exception e) {
                LogUtils.getLogger().error("[PvpCiv] Bad loot table {}: {}", id, e.getMessage());
            }
        });
        LogUtils.getLogger().info("[PvpCiv] Loaded {} civ loot tables", TABLES.size());
    }

    public static List<ItemStack> roll(String table, RandomSource rnd, double chanceMult) {
        List<ItemStack> out = new ArrayList<>();
        JsonArray entries = TABLES.get(table);
        if (entries == null) return out;
        for (JsonElement el : entries) {
            JsonObject e = el.getAsJsonObject();
            double chance = e.has("chance") ? e.get("chance").getAsDouble() : 1.0;
            if (rnd.nextDouble() >= Math.min(1.0, chance * chanceMult)) continue;
            if (e.has("weapons")) {
                JsonArray ws = e.getAsJsonArray("weapons");
                Item it = item(ws.get(rnd.nextInt(ws.size())).getAsString());
                if (it == Items.AIR) continue;
                Rarity min = e.has("min_rarity") ? Rarity.valueOf(e.get("min_rarity").getAsString()) : Rarity.COMMON;
                if (it instanceof CivWeaponItem w && w.minRarity.ordinal() > min.ordinal()) min = w.minRarity;
                boolean mythic = e.has("mythic") && e.get("mythic").getAsBoolean();
                out.add(Gear.withRarity(new ItemStack(it), Rarity.roll(rnd, min, mythic)));
            } else {
                Item it = item(e.get("item").getAsString());
                if (it == Items.AIR) continue;
                int min = e.has("min") ? e.get("min").getAsInt() : 1, max = e.has("max") ? e.get("max").getAsInt() : min;
                int n = min + (max > min ? rnd.nextInt(max - min + 1) : 0);
                while (n > 0) {
                    int c = Math.min(n, it.getDefaultMaxStackSize());
                    out.add(new ItemStack(it, c));
                    n -= c;
                }
            }
        }
        return out;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }
}
