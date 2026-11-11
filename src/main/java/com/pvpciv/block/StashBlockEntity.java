package com.pvpciv.block;

import com.pvpciv.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Нужен только для рендера крышки тайника (открыта/закрыта — у каждого игрока своё). */
public class StashBlockEntity extends BlockEntity {
    public StashBlockEntity(BlockPos pos, BlockState st) { super(ModRegistry.STASH_BE.get(), pos, st); }
}
