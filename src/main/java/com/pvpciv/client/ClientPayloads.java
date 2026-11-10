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
        ClientState.quest = p.quest();
        ClientState.questSeconds = p.questSeconds();
        ClientState.jailSeconds = p.jailSeconds();
        ClientState.titles = new java.util.HashSet<>(java.util.Arrays.asList(p.titles().split(",")));
        ClientState.activeTitle = p.activeTitle();
    }

    public static void cinematic(Network.Cinematic p) {
        ClientState.cineKind = p.kind();
        ClientState.cineStart = System.currentTimeMillis();
        ClientState.cineLen = p.ticks() * 50L;
        ClientState.cineText = p.text();
    }
}
