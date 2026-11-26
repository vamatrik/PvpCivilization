package com.pvpciv.game;

import com.pvpciv.data.StoryData;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.2: показ брони. Игрок прячет отдельные части (кнопки-«глазки» у слотов брони) — их не видно ни ему, ни другим.
 * Прячет только вид: защита остаётся. Маска (биты 0–3: шлем, нагрудник, поножи, ботинки) — у всех клиентов.
 */
public final class ArmorVis {
    private ArmorVis() {}

    public static int mask(ServerPlayer p) { return Progress.d(p).gates.getInt("arm_hide") & 15; }

    public static void set(ServerPlayer p, int m) {
        StoryData d = Progress.d(p);
        d.gates.putInt("arm_hide", m & 15);
        p.setData(ModRegistry.STORY.get(), d);
        PacketDistributor.sendToAllPlayers(packet(p));
    }

    private static Network.PhoneData packet(ServerPlayer p) {
        return new Network.PhoneData(29, "{\"e\":" + p.getId() + ",\"m\":" + mask(p) + "}");
    }

    /** Вошёл: его маска — всем, маски остальных — ему. */
    public static void login(ServerPlayer p) {
        PacketDistributor.sendToAllPlayers(packet(p));
        for (ServerPlayer o : p.server.getPlayerList().getPlayers()) if (o != p) PacketDistributor.sendToPlayer(p, packet(o));
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(6, "{\"pvpShield\":" + Progress.d(p).pvpShield + ",\"armPvp\":" + Progress.d(p).gates.getBoolean("arm_pvp") + "}"));
    }
}
