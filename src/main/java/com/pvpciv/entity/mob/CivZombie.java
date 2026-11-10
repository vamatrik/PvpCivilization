package com.pvpciv.entity.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** Зомби цивилизации (пепельный, обмороженный, Громила): не горит на солнце, не тонет, без подкреплений. */
public class CivZombie extends Zombie {
    public CivZombie(EntityType<? extends Zombie> type, Level level) { super(type, level); }
    @Override protected boolean isSunSensitive() { return false; }
    @Override protected boolean convertsInWater() { return false; }
    @Override public boolean canBreakDoors() { return false; }
    @Override public boolean canPickUpLoot() { return false; }
}
