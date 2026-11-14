package com.pvpciv.combat;

import com.pvpciv.game.GameEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Всплывающие цифры урона на базе text_display. */
public final class DamageNumbers {
    public static void show(ServerLevel lvl, Entity target, Component text) { show(lvl, target, text, 1f); }

    public static void show(ServerLevel lvl, Entity target, Component text, float scale) {
        // 0.10: цифры рисует клиент (вылетают над полоской здоровья) — сервер только сообщает, что и где
        com.google.gson.JsonObject j = new com.google.gson.JsonObject();
        j.addProperty("e", target.getId());
        j.addProperty("t", text.getString());
        var col = text.getStyle().getColor();
        j.addProperty("c", col == null ? 0xFFFFFF : col.getValue());
        j.addProperty("b", text.getStyle().isBold());
        j.addProperty("s", scale);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new com.pvpciv.net.Network.PhoneData(15, j.toString()));
    }

    private static ListTag fl(float... v) { ListTag l = new ListTag(); for (float f : v) l.add(net.minecraft.nbt.FloatTag.valueOf(f)); return l; }
}
