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

    public static Map<String, JsonArray> tables() { return java.util.Collections.unmodifiableMap(TABLES); }

    /** 1.0: перекос лута к оружию класса игрока (ставится вокруг вызова roll, сервер однопоточный). */
    public static java.util.function.Predicate<Item> PREFER;

    public static List<ItemStack> roll(String table, RandomSource rnd, double chanceMult) {
        List<ItemStack> out = new ArrayList<>();
        JsonArray entries = TABLES.get(table);
        if (entries == null) return out;
        for (JsonElement el : entries) {
            JsonObject e = el.getAsJsonObject();
            double chance = e.has("chance") ? e.get("chance").getAsDouble() : 1.0;
            if (rnd.nextDouble() >= Math.min(1.0, chance * chanceMult)) continue;
            if (e.has("weapons") || e.has("gear")) {
                JsonArray ws = e.getAsJsonArray(e.has("weapons") ? "weapons" : "gear");
                Item it = item(ws.get(rnd.nextInt(ws.size())).getAsString());
                if (PREFER != null && !PREFER.test(it) && rnd.nextDouble() < 0.5) {   // половина «чужих» бросков перекидывается на оружие класса
                    List<Item> mine = new ArrayList<>();
                    for (JsonElement we : ws) { Item wi = item(we.getAsString()); if (PREFER.test(wi)) mine.add(wi); }
                    if (!mine.isEmpty()) it = mine.get(rnd.nextInt(mine.size()));
                }
                if (it == Items.AIR) continue;
                Rarity min = e.has("min_rarity") ? Rarity.valueOf(e.get("min_rarity").getAsString()) : Rarity.COMMON;
                if (it instanceof CivWeaponItem w && w.minRarity.ordinal() > min.ordinal()) min = w.minRarity;
                boolean mythic = e.has("mythic") && e.get("mythic").getAsBoolean();
                Rarity r = Rarity.roll(rnd, min, mythic);
                // 1.2: вторая локация этажа (таблицы floorNb_*) — редкость чуть выше: обычное в 35% случаев перебрасывается от необычного
                if (r == Rarity.COMMON && table.matches("floor\\db_.*") && rnd.nextDouble() < 0.35) r = Rarity.roll(rnd, Rarity.UNCOMMON, mythic);
                out.add(Gear.withRarity(new ItemStack(it), r));
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
