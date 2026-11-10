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
    public static void show(ServerLevel lvl, Entity target, Component text) {
        double x = target.getX() + (lvl.random.nextDouble() - 0.5) * 0.8;
        double y = target.getY() + target.getBbHeight() + 0.2;
        double z = target.getZ() + (lvl.random.nextDouble() - 0.5) * 0.8;
        CompoundTag t = new CompoundTag();
        t.putString("id", "minecraft:text_display");
        ListTag pos = new ListTag();
        pos.add(DoubleTag.valueOf(x)); pos.add(DoubleTag.valueOf(y)); pos.add(DoubleTag.valueOf(z));
        t.put("Pos", pos);
        t.putString("text", Component.Serializer.toJson(text, lvl.registryAccess()));
        t.putString("billboard", "center");
        t.putString("alignment", "center");
        t.putInt("background", 0);
        t.putBoolean("shadow", true);
        t.putBoolean("see_through", true);
        t.putInt("teleport_duration", 14);
        Entity e = EntityType.loadEntityRecursive(t, lvl, en -> en);
        if (e == null) return;
        e.addTag("pvpciv_dmgnum");
        CombatEvents.LIVE_NUMBERS.add(e.getUUID());
        lvl.addFreshEntity(e);
        GameEvents.later(lvl.getServer(), 1, () -> { if (e.isAlive()) e.teleportTo(x, y + 0.9, z); });
        GameEvents.later(lvl.getServer(), 18, () -> { e.discard(); CombatEvents.LIVE_NUMBERS.remove(e.getUUID()); });
    }
}
