package com.pvpciv.world;

import com.pvpciv.PvpCiv;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Каждый мир цивилизации — отдельное пустое измерение (data/pvpciv/dimension/world1.json). */
public final class CivWorld {
    public static final ResourceKey<Level> WORLD1 = ResourceKey.create(Registries.DIMENSION, PvpCiv.id("world1"));

    public static ServerLevel level(MinecraftServer server) {
        ServerLevel l = server.getLevel(WORLD1);
        return l != null ? l : server.overworld();
    }

    public static boolean is(Level level) { return level.dimension() == WORLD1 || level.dimension() == F2 || level.dimension() == F3 || level.dimension() == F4; }

    /** Этаж 2 мира 1 — отдельное измерение (небо над этажом 1). */
    public static final ResourceKey<Level> F2 = ResourceKey.create(Registries.DIMENSION, PvpCiv.id("world1_f2"));
    public static ServerLevel f2(MinecraftServer server) {
        ServerLevel l = server.getLevel(F2);
        return l != null ? l : level(server);
    }
    public static boolean isF2(Level level) { return level.dimension() == F2; }

    /** Этаж 3 мира 1 (0.9.0) — Тихая библиотека. */
    public static final ResourceKey<Level> F3 = ResourceKey.create(Registries.DIMENSION, PvpCiv.id("world1_f3"));
    public static ServerLevel f3(MinecraftServer server) {
        ServerLevel l = server.getLevel(F3);
        return l != null ? l : level(server);
    }
    public static boolean isF3(Level level) { return level.dimension() == F3; }
    /** Этаж 4 мира 1 (0.11) — НТЦ «NaZa». */
    public static final ResourceKey<Level> F4 = ResourceKey.create(Registries.DIMENSION, PvpCiv.id("world1_f4"));
    public static ServerLevel f4(MinecraftServer server) {
        ServerLevel l = server.getLevel(F4);
        return l != null ? l : level(server);
    }
    public static boolean isF4(Level level) { return level.dimension() == F4; }
    /** Номер этажа мира 1 (0 — не мир цивилизации). */
    public static int floor(Level level) { return isF4(level) ? 4 : isF3(level) ? 3 : isF2(level) ? 2 : level.dimension() == WORLD1 ? 1 : 0; }
    public static ServerLevel byFloor(MinecraftServer s, int floor) { return floor == 4 ? f4(s) : floor == 3 ? f3(s) : floor == 2 ? f2(s) : level(s); }
}
