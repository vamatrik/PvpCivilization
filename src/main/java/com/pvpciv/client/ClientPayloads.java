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
            case 7 -> mc.setScreen(new com.pvpciv.client.screen.GateScreen(j));
            case 9 -> mc.setScreen(new com.pvpciv.client.screen.AlmanacScreen(j));
            case 19 -> {   // 0.11: молекулярный стол
                ClientState.ench = j;
                if (!(mc.screen instanceof com.pvpciv.client.screen.EnchantScreen)) mc.setScreen(new com.pvpciv.client.screen.EnchantScreen());
            }
            case 20 -> {   // 0.11: проглочен Лианой
                ClientState.grabOn = j.get("on").getAsBoolean();
                if (ClientState.grabOn) { ClientState.grabBar = j.get("bar").getAsFloat(); ClientState.grabT = j.get("t").getAsInt(); }
            }
            case 18 -> {   // навыки
                var sel = j.getAsJsonArray("sel");
                for (int i = 0; i < 3 && i < sel.size(); i++) ClientState.skillSel[i] = sel.get(i).getAsString();
                ClientState.skillsAll = j.getAsJsonArray("all");
            }
            case 11 -> {   // перезарядка слота
                int sl = j.has("slot") ? j.get("slot").getAsInt() : 2;
                long ms = j.get("cd").getAsLong() * 1000;
                ClientState.skillReady[sl - 1] = System.currentTimeMillis() + ms; ClientState.skillCdMs[sl - 1] = ms;
            }
            case 17 -> {   // взмах другого игрока
                var en = mc.level == null ? null : mc.level.getEntity(j.get("e").getAsInt());
                if (en instanceof net.minecraft.world.entity.LivingEntity le && le != mc.player) Lines.slash(le, j.get("k").getAsInt(), j.get("o").getAsBoolean(), j.get("c").getAsInt());
            }
            case 16 -> {   // линии: a/b — концы, c — цвет, w — толщина, l — жизнь (мс), z — лазер
                var a = j.getAsJsonArray("a"); var b = j.getAsJsonArray("b");
                Lines.add(new net.minecraft.world.phys.Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()),
                        new net.minecraft.world.phys.Vec3(b.get(0).getAsDouble(), b.get(1).getAsDouble(), b.get(2).getAsDouble()),
                        j.get("c").getAsInt(), j.get("w").getAsFloat(), j.get("l").getAsInt(), j.get("z").getAsBoolean());
            }
            case 15 -> Nums.add(j.get("e").getAsInt(), j.get("t").getAsString(), j.get("c").getAsInt(), j.get("b").getAsBoolean(), j.get("s").getAsFloat());
            case 14 -> { ClientState.bleed = j.get("n").getAsInt(); if (j.get("burst").getAsBoolean()) ClientState.bleedBurst = System.currentTimeMillis(); }
            case 13 -> { if (mc.screen instanceof com.pvpciv.client.screen.WordScreen) mc.setScreen(null); }
            case 12 -> { if (mc.screen instanceof com.pvpciv.client.screen.WordScreen ws) ws.reject(); }
            case 10 -> { if (mc.screen instanceof com.pvpciv.client.screen.WordScreen ws) ws.data(j); else mc.setScreen(new com.pvpciv.client.screen.WordScreen(j)); }
            case 8 -> { ClientState.gatesFixed.clear(); for (var el : j.getAsJsonArray("fixed")) ClientState.gatesFixed.add(el.getAsString()); }
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
