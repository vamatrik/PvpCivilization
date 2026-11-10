package com.pvpciv.world;

import com.pvpciv.data.CivData;
import com.pvpciv.floor.Floor1;
import com.pvpciv.floor.FloorNorth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;

/** Полная сборка мира 1 в его измерении: общежитие, подвал, обе локации этажа 1, трассы. */
public final class WorldBuild {
    public static final int VERSION = 10;

    public static void buildAll(MinecraftServer server, boolean clear) {
        var log = com.mojang.logging.LogUtils.getLogger();
        ServerLevel lvl = CivWorld.level(server);
        CivData data = CivData.get(server);
        // владельцы квартир сохраняются по положению двери
        Map<BlockPos, CivData.Apartment> old = new HashMap<>();
        for (CivData.Apartment a : data.apartments) old.put(a.door, a);
        long t = System.currentTimeMillis();
        DormBuilder.build(lvl, data);
        for (CivData.Apartment a : data.apartments) {
            CivData.Apartment o = old.get(a.door);
            if (o != null && o.owner != null && a.npc.isEmpty()) {
                a.owner = o.owner; a.ownerName = o.ownerName;
                DormBuilder.updateApartmentSign(lvl, a);
            }
        }
        Basement.build(lvl);
        log.info("[PvpCiv] Dormitory + basement: {} ms", System.currentTimeMillis() - t);
        t = System.currentTimeMillis();
        Floor1.build(lvl, clear);
        log.info("[PvpCiv] Floor 1 / Ash Wastes: {} ms", System.currentTimeMillis() - t);
        t = System.currentTimeMillis();
        FloorNorth.build(lvl, clear);
        IceTrack.build(lvl);
        log.info("[PvpCiv] Floor 1 / Rime Ridge + Ice Track: {} ms", System.currentTimeMillis() - t);
        DormBuilder.placeForges(lvl, data);
        DormBuilder.placeDismantlers(lvl, data);
        // при стройке отрываются цветы/свечи/ковры — убрать выпавшие предметы, чтобы не лагало
        java.util.List<net.minecraft.world.entity.Entity> drops = new java.util.ArrayList<>();
        for (var e : lvl.getAllEntities()) if (e instanceof net.minecraft.world.entity.item.ItemEntity) drops.add(e);
        drops.forEach(net.minecraft.world.entity.Entity::discard);
        log.info("[PvpCiv] Removed {} dropped items after build", drops.size());
        data.version = VERSION;
        data.built = true;
        data.setDirty();
    }
}
