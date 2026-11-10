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
        ClientState.zone = p.zone();
        ClientState.zoneType = p.zoneType();
        ClientState.bossSeconds = p.bossSeconds();
    }
}
