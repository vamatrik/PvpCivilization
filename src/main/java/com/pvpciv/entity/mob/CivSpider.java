package com.pvpciv.entity.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;

public class CivSpider extends Spider {
    public CivSpider(EntityType<? extends Spider> type, Level level) { super(type, level); }
    /** Мобы цивилизации не дерутся между собой — цель только игрок. */
    @Override public void setTarget(net.minecraft.world.entity.LivingEntity t) {
        if (t != null && !(t instanceof net.minecraft.world.entity.player.Player)) return;
        super.setTarget(t);
    }
}
