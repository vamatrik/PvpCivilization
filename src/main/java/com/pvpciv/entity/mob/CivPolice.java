package com.pvpciv.entity.mob;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** Полицейский с дубинкой: каждый 4-й удар примерно — оглушение на 1 с. */
public class CivPolice extends CivZombie {
    public CivPolice(EntityType<? extends Zombie> type, Level level) { super(type, level); }

    @Override
    public boolean doHurtTarget(Entity e) {
        boolean hit = super.doHurtTarget(e);
        if (hit && e instanceof LivingEntity t && random.nextFloat() < 0.25f) com.pvpciv.game.WeaponSkills.stun(t, 20);
        return hit;
    }
}
