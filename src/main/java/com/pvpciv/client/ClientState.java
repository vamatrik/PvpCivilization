package com.pvpciv.client;

public final class ClientState {
    public static int stage = 0;
    public static String objective = "";
    public static int level = 1;
    public static long xp = 0, xpNext = 60, money = 0;
    public static String zone = "";
    public static int zoneType = 0;
    public static int bossSeconds = -1;
    public static long dashReadyAt = 0;
    public static long objectiveFlashUntil = 0;
    public static long moneyFlashUntil = 0;
    public static java.util.List<com.pvpciv.net.Network.OfferView> offers = java.util.List.of();
    public static long shopRotationAt = 0;
    public static long zoneChangedAt = 0;
    public static com.pvpciv.net.Network.DeathInfo death = null;
    public static String quest = "";
    public static int questSeconds = -1, jailSeconds = -1;
    public static java.util.Set<String> titles = new java.util.HashSet<>();
    public static String activeTitle = "";
    /** Катсцена: вид, начало (мс), длительность (мс), текст. */
    public static int cineKind = 0;
    public static long cineStart = 0, cineLen = 0;
    public static String cineText = "";

    public static void reset() {
        quest = ""; questSeconds = -1; jailSeconds = -1; cineKind = 0;
        stage = 0; objective = ""; level = 1; xp = 0; money = 0; zone = ""; zoneType = 0; bossSeconds = -1; dashReadyAt = 0;
    }
}
