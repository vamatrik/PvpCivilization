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
    }
}
