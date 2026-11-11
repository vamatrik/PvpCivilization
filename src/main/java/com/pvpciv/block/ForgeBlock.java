package com.pvpciv.block;

import com.pvpciv.game.Forge;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Кузница квартиры: заточка оружия звёздами. */
public class ForgeBlock extends Block {
    public ForgeBlock(Properties p) {
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp)
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new com.pvpciv.menu.ForgeMenu(id, inv, pos), Component.literal("Кузница")));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
