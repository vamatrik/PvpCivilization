package com.pvpciv.block;

import com.pvpciv.menu.DismantleMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Разборочный стол: обломки и ненужное оружие → материалы. */
public class DismantleBlock extends Block {
    public DismantleBlock(Properties p) { super(p); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp)
            sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new DismantleMenu(id, inv), Component.literal("Разборочный стол")));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
