package com.pvpciv.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Русские названия на стороне сервера: выделенный сервер знает только английский (бета 0.8.7: «rime shard»). */
public final class Ru {
    private static Map<String, String> MAP;

    private static Map<String, String> map() {
        if (MAP != null) return MAP;
        Map<String, String> m = new HashMap<>();
        try (var in = Ru.class.getResourceAsStream("/assets/pvpciv/lang/ru_ru.json")) {
            if (in != null) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (var e : o.entrySet()) m.put(e.getKey(), e.getValue().getAsString());
            }
        } catch (Exception ignored) {}
        return MAP = m;
    }

    public static String key(String k, String fallback) { return map().getOrDefault(k, fallback); }

    public static String name(Item it) { return key(it.getDescriptionId(), it.getDescription().getString()); }

    public static String name(ItemStack st) {
        if (st.has(DataComponents.CUSTOM_NAME) || st.has(DataComponents.ITEM_NAME)) return st.getHoverName().getString();
        return key(st.getDescriptionId(), st.getHoverName().getString());
    }
}
