package com.pvpciv.client;

import com.pvpciv.net.Network;
import net.minecraft.client.Minecraft;

public final class ClientPayloads {
    public static void dialogueOpen(Network.DialogueOpen p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DialogueScreen ds) ds.update(p);
        else mc.setScreen(new DialogueScreen(p));
    }

    public static void dialogueClose() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DialogueScreen ds) ds.closeFromServer();
    }

    public static void offers(Network.TraderOffers p) {
        ClientState.offers = p.offers();
        ClientState.shopRotationAt = System.currentTimeMillis() + p.msLeft();
        ClientState.shopTokens = p.tokens();
    }

    public static void death(Network.DeathInfo p) { ClientState.death = p; }

    public static void sync(Network.SyncState p) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level != null ? mc.level.getGameTime() : 0;
        if (!p.objective().equals(ClientState.objective)) ClientState.objectiveFlashUntil = now + 60;
        if (p.money() != ClientState.money) ClientState.moneyFlashUntil = now + 20;
        ClientState.stage = p.stage();
        ClientState.objective = p.objective();
        ClientState.level = p.level();
        ClientState.xp = p.xp();
        ClientState.xpNext = p.xpNext();
        ClientState.money = p.money();
        if (!p.zone().equals(ClientState.zone) && !ClientState.zone.isEmpty() && !p.zone().equals("Бездна")) ClientState.zoneChangedAt = System.currentTimeMillis();
        ClientState.zone = p.zone();
        ClientState.zoneType = p.zoneType();
        ClientState.bossSeconds = p.bossSeconds();
        ClientState.keeperSeconds = p.keeperSeconds();
        ClientState.bossTimers.clear();
        for (String e : p.bosses().split(",")) { int i = e.indexOf(':'); if (i > 0) ClientState.bossTimers.put(e.substring(0, i), Integer.parseInt(e.substring(i + 1))); }
        ClientState.trophies = p.trophies();
        String[] bt = p.boats().split("\\|", -1);
        ClientState.boats = bt[0].isEmpty() ? java.util.List.of() : java.util.List.of(bt[0].split(","));
        ClientState.activeBoat = bt.length > 1 ? bt[1] : "";
        ClientState.quest = p.quest();
        ClientState.questSeconds = p.questSeconds();
        ClientState.jailSeconds = p.jailSeconds();
        ClientState.titles = new java.util.HashSet<>(java.util.Arrays.asList(p.titles().split(",")));
        ClientState.activeTitle = p.activeTitle();
    }

    public static void stash(Network.StashState p) {
        ClientState.stash.clear();
        for (int i = 0; i < p.pos().size(); i++) ClientState.stash.put(p.pos().get(i), p.readyMs().get(i));
    }

    public static void cinematic(Network.Cinematic p) {
        ClientState.cineKind = p.kind();
        ClientState.cineStart = System.currentTimeMillis();
        ClientState.cineLen = p.ticks() * 50L;
        ClientState.cineText = p.text();
    }

    public static void openLift(com.pvpciv.net.Network.OpenLift p) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new com.pvpciv.client.screen.LiftScreen(p.mask(), p.current()));
    }

    public static void openMilestones(com.pvpciv.net.Network.OpenMilestones p) {
        long[] v = p.values().stream().mapToLong(Long::longValue).toArray();
        int[] c = p.claimed().stream().mapToInt(Integer::intValue).toArray();
        net.minecraft.client.Minecraft.getInstance().setScreen(new com.pvpciv.client.screen.MilestonesScreen(v, c));
    }

    public static void phone(com.pvpciv.net.Network.PhoneData p) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        com.google.gson.JsonObject j = com.google.gson.JsonParser.parseString(p.json()).getAsJsonObject();
        switch (p.kind()) {
            case 0, 1, 2 -> {
                if (mc.screen instanceof com.pvpciv.client.screen.PhoneScreen ps) ps.data(p.kind(), j);
                else mc.setScreen(new com.pvpciv.client.screen.PhoneScreen(p.kind(), j));
            }
            case 6 -> ClientState.pvpShield = j.has("pvpShield") && j.get("pvpShield").getAsBoolean();
            case 3 -> ClientState.callUntil = System.currentTimeMillis() + (j.has("sec") ? j.get("sec").getAsInt() : 10) * 1000L;
            case 4 -> ClientState.callUntil = 0;
            case 5 -> {
                if (!j.has("name")) { ClientState.taxiUntil = 0; return; }
                ClientState.taxiFrom = j.get("name").getAsString();
                ClientState.taxiTo = j.get("to").getAsString();
                ClientState.taxiUntil = System.currentTimeMillis() + j.get("sec").getAsLong() * 1000L;
                ClientState.taxiHere = j.get("here").getAsBoolean();
            }
            default -> {}
        }
    }
}
