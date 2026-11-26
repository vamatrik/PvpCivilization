package com.pvpciv.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pvpciv.data.StoryData;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.neoforged.neoforge.network.PacketDistributor;

/** 1.2: вкладка «О себе» — статистика героя. */
public final class About {
    private About() {}

    public static void send(ServerPlayer p) {
        StoryData d = Progress.d(p);
        JsonArray rows = new JsonArray();
        int kills = 0;
        for (String k : d.kills.getAllKeys()) kills += d.kills.getInt(k);
        int elites = 0, bosses = 0;
        for (String k : d.bestiary.getAllKeys()) { if (k.endsWith("_e")) elites += d.bestiary.getInt(k); if (k.startsWith("boss_")) bosses += d.bestiary.getInt(k); }
        int trophies = Integer.bitCount(d.trophies);
        int gates = 0;
        for (Gates.Gate g : Gates.ALL) if (Gates.fixed(d, g.id())) gates++;
        long play = p.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) / 20;
        Classes.PClass c = Classes.cls(d);
        row(rows, "Герой", null);
        row(rows, "Уровень", String.valueOf(d.level));
        row(rows, "Класс", c == null ? "не выбран" : c.title);
        row(rows, "Титулов", String.valueOf(d.titles.size()));
        row(rows, "В игре", play / 3600 + " ч " + play / 60 % 60 + " мин");
        row(rows, "Бой", null);
        row(rows, "Убито мобов", String.valueOf(kills));
        row(rows, "Элиток", String.valueOf(elites));
        row(rows, "Побед над боссами", String.valueOf(bosses));
        row(rows, "Трофеев", trophies + " из 10");
        row(rows, "Ударов", String.valueOf(d.hits));
        row(rows, "Смертей", String.valueOf(d.deaths));
        row(rows, "Побед на арене", String.valueOf(d.arenaWins));
        row(rows, "Испытания Луны", "стадия " + d.gates.getInt("luna_best"));
        row(rows, "Мир", null);
        row(rows, "Пройдено", String.format("%.1f км", d.distM / 1000.0));
        row(rows, "Тайников открыто", String.valueOf(d.stashesOpened));
        row(rows, "Контрактов Кармиллы", String.valueOf(d.questsDone));
        row(rows, "Врат починено", gates + " из " + Gates.ALL.size());
        row(rows, "Очки Элеоноры", String.valueOf(d.eleTokens));
        row(rows, "Искры", com.pvpciv.combat.Balance.fmt(d.money));
        JsonObject j = new JsonObject();
        j.addProperty("name", p.getGameProfile().getName());
        j.add("rows", rows);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(30, j.toString()));
    }

    private static void row(JsonArray a, String k, String v) {
        JsonObject o = new JsonObject();
        o.addProperty("k", k);
        if (v != null) o.addProperty("v", v);
        a.add(o);
    }
}
