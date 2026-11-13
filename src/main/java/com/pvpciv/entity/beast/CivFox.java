package com.pvpciv.entity.beast;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Лиса-оборотень (деревня Кумихо, этаж 3): быстрая, юркая. */
public class CivFox extends CivBeast {
    public CivFox(EntityType<? extends Monster> type, Level level) { super(type, level, "fox_spirit"); }
    @Override public float getPickRadius() { return 0.3f; }
}
