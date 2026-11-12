package com.pvpciv.client;

public final class ClientState {
    public static int stage = 0;
    public static String objective = "";
    public static int level = 1;
    public static long xp = 0, xpNext = 60, money = 0;
    public static String zone = "";
    public static int zoneType = 0;
    public static int bossSeconds = -1, keeperSeconds = -1, trophies = 0;
    public static java.util.List<String> boats = java.util.List.of();
    public static String activeBoat = "";
    public static final java.util.Map<String, Integer> bossTimers = new java.util.HashMap<>();
    public static int bossSeconds(String id) { return bossTimers.getOrDefault(id, id.equals("keeper") ? keeperSeconds : bossSeconds); }
    public static long dashReadyAt = 0;
    public static long objectiveFlashUntil = 0;
    public static long moneyFlashUntil = 0;
    public static java.util.List<com.pvpciv.net.Network.OfferView> offers = java.util.List.of();
    public static long shopRotationAt = 0;
    public static long zoneChangedAt = 0;
    public static com.pvpciv.net.Network.DeathInfo death = null;
    public static String quest = "";
    /** Обчищенные тайники: позиция → когда снова готовы (мс). */
    public static final java.util.Map<Long, Long> stash = new java.util.HashMap<>();
    public static boolean stashLooted(net.minecraft.core.BlockPos p) { Long t = stash.get(p.asLong()); return t != null && t > System.currentTimeMillis(); }
    /** Закреплённые на HUD боссы (сохраняется в config/pvpciv_pins.txt). */
    private static java.util.Set<String> pins;
    private static java.nio.file.Path pinFile() { return net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath().resolve("config/pvpciv_pins.txt"); }
    public static boolean pinned(String id) {
        if (pins == null) {
            pins = new java.util.HashSet<>();
            try { for (String l : java.nio.file.Files.readAllLines(pinFile())) if (!l.isBlank()) pins.add(l.trim()); } catch (Exception ignored) {}
        }
        return pins.contains(id);
    }
    public static void togglePin(String id) {
        if (pinned(id)) pins.remove(id); else pins.add(id);
        try { java.nio.file.Files.write(pinFile(), pins); } catch (Exception ignored) {}
    }
    public static int questSeconds = -1, jailSeconds = -1;
    public static java.util.Set<String> titles = new java.util.HashSet<>();
    public static String activeTitle = "";
    /** Катсцена: вид, начало (мс), длительность (мс), текст. */
    public static int cineKind = 0;
    public static long cineStart = 0, cineLen = 0;
    public static String cineText = "";

    /** Лавка Элеоноры: ≥0 — цены в жетонах (баланс). */
    public static int shopTokens = -1;
    public static boolean pvpShield = false;
    /** Входящий звонок (до какого момента звонит, мс) и ожидающее такси. */
    public static long callUntil = 0;
    public static String taxiFrom = "", taxiTo = "";
    public static long taxiUntil = 0;
    public static boolean taxiHere = false;

    public static void reset() {
        callUntil = 0; taxiUntil = 0; shopTokens = -1;
        quest = ""; questSeconds = -1; jailSeconds = -1; cineKind = 0;
        stage = 0; objective = ""; level = 1; xp = 0; money = 0; zone = ""; zoneType = 0; bossSeconds = -1; keeperSeconds = -1; trophies = 0; dashReadyAt = 0;
    }
}
