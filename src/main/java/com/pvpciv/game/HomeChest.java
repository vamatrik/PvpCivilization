package com.pvpciv.game;

import com.pvpciv.data.CivData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** Домашний сундук в квартире: личное хранилище (45 слотов), содержимое у каждого своё. Чужой квартирный сундук не открыть. */
public final class HomeChest {
    public static final int SIZE = 45;

    public static boolean canUse(ServerPlayer p, BlockPos pos) {
        for (CivData.Apartment a : CivData.get(p.server).apartments) {
            if (a.door.distSqr(pos) > 49 || Math.abs(a.door.getY() - pos.getY()) > 2) continue;
            return a.owner == null || a.owner.equals(p.getUUID()) || p.hasPermissions(2);
        }
        return true;
    }
}
