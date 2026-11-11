package com.pvpciv.entity.beast;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Канализационная крыса (враг). */
public class CivRat extends CivBeast {
    public CivRat(EntityType<? extends Monster> type, Level level) { super(type, level, "rat"); }
}
