package com.pvpciv.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 1.1: пульты вместо ванильных «интерактивных» блоков. Действие — по позиции (обработчики ПКМ в GameEvents и др.). */
public class ConsoleBlock extends HorizontalDirectionalBlock {
    public enum Kind implements StringRepresentable {
        STATION, LAB, STAND, TELEPORT;
        @Override public String getSerializedName() { return name().toLowerCase(); }
    }
    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
    public static final MapCodec<ConsoleBlock> CODEC = simpleCodec(ConsoleBlock::new);

    public ConsoleBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(KIND, Kind.STATION));
    }
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, KIND); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()); }

    private static final VoxelShape STATION = Block.box(1, 0, 1, 15, 15, 15), LAB = Block.box(0, 0, 0, 16, 13, 16),
            STAND = Block.box(3, 0, 3, 13, 14, 13), TELE = Block.box(3, 0, 3, 13, 16, 13);
    @Override protected VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return switch (s.getValue(KIND)) { case STATION -> STATION; case LAB -> LAB; case STAND -> STAND; case TELEPORT -> TELE; };
    }

    public static BlockState of(Kind k, Direction f) {
        return com.pvpciv.registry.ModV11.CONSOLE.get().defaultBlockState().setValue(KIND, k).setValue(FACING, f);
    }
}
