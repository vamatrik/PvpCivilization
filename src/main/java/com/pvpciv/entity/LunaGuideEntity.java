package com.pvpciv.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** Луна у Небесного лифта: появляется только для одного игрока (сюжетный разговор), потом исчезает. */
public class LunaGuideEntity extends GeoNpc {
    public UUID onlyFor;
    public int life = 1200;

    public LunaGuideEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, "Луна", 0x9A5AE0);
        setNoGravity(false);
    }

    @Override public String model() { return "luna"; }
    @Override public String texture() { return "luna_hd"; }
    @Override protected String idleAnim() { return "cross"; }

    @Override public boolean broadcastToPlayer(ServerPlayer p) { return onlyFor == null || onlyFor.equals(p.getUUID()); }
    @Override public boolean shouldBeSaved() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel l && (--life <= 0 || (onlyFor != null && l.getPlayerByUUID(onlyFor) == null))) {
            CarmillaEntity.puff(l, position());
            discard();
        }
    }

    @Override
    protected void onTalk(ServerPlayer p) {
        if (onlyFor != null && onlyFor.equals(p.getUUID()) && !com.pvpciv.game.DialogueManager.inDialogue(p))
            com.pvpciv.game.SkyLift.lunaTalk(p, this);
    }
}
