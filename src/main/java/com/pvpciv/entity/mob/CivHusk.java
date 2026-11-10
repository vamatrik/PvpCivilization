package com.pvpciv.entity.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

public class CivHusk extends Husk {
    public CivHusk(EntityType<? extends Husk> type, Level level) { super(type, level); }
    @Override protected boolean convertsInWater() { return false; }
    @Override public boolean canPickUpLoot() { return false; }
}
