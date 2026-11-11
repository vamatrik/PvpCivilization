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
public class StashBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
    public StashBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.NORTH));
    }

    /** Блок смотрит «лицом» на того, кто ставит. */
    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> b) {
        b.add(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        return defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState s, net.minecraft.world.level.block.Rotation r) {
        return s.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, r.rotate(s.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) Stash.open(sp, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState st) { return new StashBlockEntity(pos, st); }
}
