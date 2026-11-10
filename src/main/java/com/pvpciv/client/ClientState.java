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

    public static void reset() {
        stage = 0; objective = ""; level = 1; xp = 0; money = 0; zone = ""; zoneType = 0; bossSeconds = -1; dashReadyAt = 0;
    }
}
