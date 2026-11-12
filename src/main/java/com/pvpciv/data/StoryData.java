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
    /** Трофеи, выставленные на витрину (биты как у trophies); liftOk — Луна пустила к лифту. */
    public int showcase = 0;
    public boolean liftOk = false;
    /** Элеонора: 0 — не знакомы, 1 — квест на Крысиного короля, 2 — выполнен (телефон). КД приложений телефона. */
    public int eleStage = 0;
    public net.minecraft.nbt.CompoundTag phoneCd = new net.minecraft.nbt.CompoundTag();
    /** Метки Элеоноры: жетоны (вне инфляции), активная метка "boss:TYPE" и день её выдачи, сданные метки (boss → день), покупки в её лавке. */
    /** Настройки героя: не драться с игроками на 7+ уровней выше (в обе стороны). */
    public boolean pvpShield = false;
    public int eleTokens = 0;
    public String eleMark = "";
    public long eleMarkDay = 0;
    public net.minecraft.nbt.CompoundTag eleDone = new net.minecraft.nbt.CompoundTag(), eleShop = new net.minecraft.nbt.CompoundTag();
    /** «Шпиль.Еда»: текущий заказ и раскупленный склад. */
    public net.minecraft.nbt.CompoundTag food = new net.minecraft.nbt.CompoundTag(), foodStock = new net.minecraft.nbt.CompoundTag();
    /** Счётчики «дневника подвигов» Тимофея и сданные вехи. */
    public long hits = 0, distM = 0, stashesOpened = 0;
    public net.minecraft.nbt.CompoundTag milestones = new net.minecraft.nbt.CompoundTag();
    public int forgeLvl = 1, dismLvl = 1, extraSlots = 0;
    public boolean starterFood = false;
    public CompoundTag shop = new CompoundTag();
    // титулы
    public java.util.Set<String> titles = new java.util.LinkedHashSet<>();
    public String activeTitle = "";
    /** Гараж: открытые лодки (id предметов) и выбранная. */
    public java.util.Set<String> boats = new java.util.LinkedHashSet<>();
    public String activeBoat = "";
    // контракты Кармиллы
    public String questKind = "";       // Mobs.Kind.name() или "ELITE"
    public int questNeed = 0, questHave = 0, questTier = 0, questsDone = 0, jailed = 0;
    public long questDeadline = 0, questCooldown = 0, jailUntil = 0;
    public boolean cdFail = false;
    public boolean boughtBuckle = false, boughtPack = false;
    public long eliteCd = 0;
    public CompoundTag kills = new CompoundTag();
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
        t.putInt("showcase", showcase);
        t.putBoolean("liftOk", liftOk);
        t.putInt("eleStage", eleStage); t.put("phoneCd", phoneCd);
        t.putBoolean("pvpShield", pvpShield); t.putInt("eleTokens", eleTokens); t.putString("eleMark", eleMark); t.putLong("eleMarkDay", eleMarkDay); t.put("eleDone", eleDone); t.put("eleShop", eleShop);
        t.put("food", food); t.put("foodStock", foodStock);
        t.putLong("hits", hits); t.putLong("distM", distM); t.putLong("stashesOpened", stashesOpened);
        t.put("milestones", milestones);
        t.putInt("forgeLvl", forgeLvl);
        t.putInt("dismLvl", dismLvl);
        t.putInt("extraSlots", extraSlots);
        t.putBoolean("starterFood", starterFood);
        t.put("shop", shop);
        t.putString("titles", String.join(",", titles));
        t.putString("activeTitle", activeTitle);
        t.putString("boats", String.join(",", boats));
        t.putString("activeBoat", activeBoat);
        t.putString("questKind", questKind);
        t.putInt("questNeed", questNeed); t.putInt("questHave", questHave); t.putInt("questTier", questTier);
        t.putInt("questsDone", questsDone); t.putInt("jailed", jailed);
        t.putLong("questDeadline", questDeadline); t.putLong("questCooldown", questCooldown); t.putLong("jailUntil", jailUntil);
        t.putBoolean("lunaBox", lunaBox); t.putBoolean("cdFail", cdFail); t.putBoolean("boughtBuckle", boughtBuckle); t.putBoolean("boughtPack", boughtPack); t.putLong("eliteCd", eliteCd); t.put("kills", kills); t.putInt("lunaCaught", lunaCaught);
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
        showcase = t.getInt("showcase");
        liftOk = t.getBoolean("liftOk");
        eleStage = t.getInt("eleStage"); phoneCd = t.getCompound("phoneCd");
        pvpShield = t.getBoolean("pvpShield"); eleTokens = t.getInt("eleTokens"); eleMark = t.getString("eleMark"); eleMarkDay = t.getLong("eleMarkDay"); eleDone = t.getCompound("eleDone"); eleShop = t.getCompound("eleShop");
        food = t.getCompound("food"); foodStock = t.getCompound("foodStock");
        hits = t.getLong("hits"); distM = t.getLong("distM"); stashesOpened = t.getLong("stashesOpened");
        milestones = t.getCompound("milestones");
        forgeLvl = Math.max(1, t.getInt("forgeLvl"));
        dismLvl = Math.max(1, t.getInt("dismLvl"));
        extraSlots = t.getInt("extraSlots");
        starterFood = t.getBoolean("starterFood");
        shop = t.getCompound("shop");
        titles = new java.util.LinkedHashSet<>();
        for (String x : t.getString("titles").split(",")) if (!x.isBlank()) titles.add(x);
        activeTitle = t.getString("activeTitle");
        boats = new java.util.LinkedHashSet<>();
        for (String x : t.getString("boats").split(",")) if (!x.isBlank()) boats.add(x);
        activeBoat = t.getString("activeBoat");
        questKind = t.getString("questKind");
        questNeed = t.getInt("questNeed"); questHave = t.getInt("questHave"); questTier = t.getInt("questTier");
        questsDone = t.getInt("questsDone"); jailed = t.getInt("jailed");
        questDeadline = t.getLong("questDeadline"); questCooldown = t.getLong("questCooldown"); jailUntil = t.getLong("jailUntil");
        lunaBox = t.getBoolean("lunaBox"); cdFail = t.getBoolean("cdFail"); boughtBuckle = t.getBoolean("boughtBuckle"); boughtPack = t.getBoolean("boughtPack"); eliteCd = t.getLong("eliteCd"); kills = t.getCompound("kills"); lunaCaught = t.getInt("lunaCaught");
    }
}
