package com.pvpciv.entity.beast;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Бешеная дворняга (улица этажа 2). */
public class CivDog extends CivBeast {
    public CivDog(EntityType<? extends Monster> type, Level level) { super(type, level, "street_dog"); }

    /** С беты: по дворнягам трудно попасть — хитбокс шире и +0.35 к «зоне попадания» курсором. */
    @Override public float getPickRadius() { return 0.35f; }
}
