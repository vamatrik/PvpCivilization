package com.pvpciv.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Персональный сюжетный прогресс игрока. Переживает смерть. */
public class StoryData implements INBTSerializable<CompoundTag> {
    public int stage = 0;
    public int apartment = -1;
    public boolean dash = false;
    public int arenaWins = 0;
    public int deaths = 0;
    public int level = 1;
    public long xp = 0;
    public long money = 0;
    public int lunaLosses = 0;
    public int trophies = 0;
    public int forgeLvl = 1, dismLvl = 1, extraSlots = 0;
    public boolean starterFood = false;
    public CompoundTag shop = new CompoundTag();
    // титулы
    public java.util.Set<String> titles = new java.util.LinkedHashSet<>();
    public String activeTitle = "";
    // контракты Кармиллы
    public String questKind = "";       // Mobs.Kind.name() или "ELITE"
    public int questNeed = 0, questHave = 0, questTier = 0, questsDone = 0, jailed = 0;
    public long questDeadline = 0, questCooldown = 0, jailUntil = 0;
    public boolean cdFail = false;
    // Луна
    public boolean lunaBox = false;
    public int lunaCaught = 0;

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag t = new CompoundTag();
        t.putInt("stage", stage);
        t.putInt("apartment", apartment);
        t.putBoolean("dash", dash);
        t.putInt("arenaWins", arenaWins);
        t.putInt("deaths", deaths);
        t.putInt("level", level);
        t.putLong("xp", xp);
        t.putLong("money", money);
        t.putInt("lunaLosses", lunaLosses);
        t.putInt("trophies", trophies);
        t.putInt("forgeLvl", forgeLvl);
        t.putInt("dismLvl", dismLvl);
        t.putInt("extraSlots", extraSlots);
        t.putBoolean("starterFood", starterFood);
        t.put("shop", shop);
        t.putString("titles", String.join(",", titles));
        t.putString("activeTitle", activeTitle);
        t.putString("questKind", questKind);
        t.putInt("questNeed", questNeed); t.putInt("questHave", questHave); t.putInt("questTier", questTier);
        t.putInt("questsDone", questsDone); t.putInt("jailed", jailed);
        t.putLong("questDeadline", questDeadline); t.putLong("questCooldown", questCooldown); t.putLong("jailUntil", jailUntil);
        t.putBoolean("lunaBox", lunaBox); t.putBoolean("cdFail", cdFail); t.putInt("lunaCaught", lunaCaught);
        return t;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag t) {
        stage = t.getInt("stage");
        apartment = t.contains("apartment") ? t.getInt("apartment") : -1;
        dash = t.getBoolean("dash");
        arenaWins = t.getInt("arenaWins");
        deaths = t.getInt("deaths");
        level = Math.max(1, t.getInt("level"));
        xp = t.getLong("xp");
        money = t.getLong("money");
        lunaLosses = t.getInt("lunaLosses");
        trophies = t.getInt("trophies");
        forgeLvl = Math.max(1, t.getInt("forgeLvl"));
        dismLvl = Math.max(1, t.getInt("dismLvl"));
        extraSlots = t.getInt("extraSlots");
        starterFood = t.getBoolean("starterFood");
        shop = t.getCompound("shop");
        titles = new java.util.LinkedHashSet<>();
        for (String x : t.getString("titles").split(",")) if (!x.isBlank()) titles.add(x);
        activeTitle = t.getString("activeTitle");
        questKind = t.getString("questKind");
        questNeed = t.getInt("questNeed"); questHave = t.getInt("questHave"); questTier = t.getInt("questTier");
        questsDone = t.getInt("questsDone"); jailed = t.getInt("jailed");
        questDeadline = t.getLong("questDeadline"); questCooldown = t.getLong("questCooldown"); jailUntil = t.getLong("jailUntil");
        lunaBox = t.getBoolean("lunaBox"); cdFail = t.getBoolean("cdFail"); lunaCaught = t.getInt("lunaCaught");
    }
}
