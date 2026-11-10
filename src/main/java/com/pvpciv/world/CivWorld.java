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

    public static boolean is(Level level) { return level.dimension() == WORLD1; }
}
