package com.pvpciv.block;

import com.pvpciv.game.Stash;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Тайник: личный лут для каждого игрока, обновляется по таймеру. */
public class StashBlock extends Block {
    public StashBlock(Properties p) { super(p); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) Stash.open(sp, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
