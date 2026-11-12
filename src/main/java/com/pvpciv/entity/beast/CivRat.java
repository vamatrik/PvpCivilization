package com.pvpciv.entity.beast;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Канализационная крыса (враг). */
public class CivRat extends CivBeast {
    public CivRat(EntityType<? extends Monster> type, Level level) { super(type, level, "rat"); }

    /** С беты: по крысам трудно попасть — хитбокс 0.85×0.6 и ещё +0.45 к «зоне попадания» курсором. */
    @Override public float getPickRadius() { return 0.45f; }
}
