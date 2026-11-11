package com.pvpciv.entity.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;

/** Лучник цивилизации (пепельный и инеевый). */
public class CivSkeleton extends Skeleton {
    public CivSkeleton(EntityType<? extends Skeleton> type, Level level) { super(type, level); }
    @Override protected boolean isSunBurnTick() { return false; }
    @Override public boolean isFreezeConverting() { return false; }
    @Override public boolean canPickUpLoot() { return false; }
    @SuppressWarnings("unused") private static final Class<?> C = AbstractSkeleton.class;
    /** Мобы цивилизации не дерутся между собой — цель только игрок. */
    @Override public void setTarget(net.minecraft.world.entity.LivingEntity t) {
        if (t != null && !(t instanceof net.minecraft.world.entity.player.Player)) return;
        super.setTarget(t);
    }
}
