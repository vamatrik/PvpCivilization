package com.pvpciv.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.1: табличка в стиле локации (0 — общежитие, 1..5 — этажи). Нередактируемая.
 * STANDING — на стойке посреди площадки, иначе — на стене (FACING — куда смотрит лицевая сторона).
 */
public class CivSignBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final IntegerProperty STYLE = IntegerProperty.create("style", 0, 5);
    public static final BooleanProperty STANDING = BooleanProperty.create("standing");
    public static final MapCodec<CivSignBlock> CODEC = simpleCodec(CivSignBlock::new);

    public CivSignBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STYLE, 1).setValue(STANDING, false));
    }
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, STYLE, STANDING); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState st) { return new CivSignBlockEntity(pos, st); }

    private static final VoxelShape[] WALL = new VoxelShape[4];
    private static final VoxelShape POST = Block.box(5, 0, 5, 11, 16, 11);
    static {
        for (Direction d : Direction.Plane.HORIZONTAL) WALL[d.get2DDataValue()] = switch (d) {
            case NORTH -> Block.box(0, 3, 14, 16, 13, 16);
            case SOUTH -> Block.box(0, 3, 0, 16, 13, 2);
            case EAST -> Block.box(0, 3, 0, 2, 13, 16);
            default -> Block.box(14, 3, 0, 16, 13, 16);
        };
    }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return s.getValue(STANDING) ? POST : WALL[s.getValue(FACING).get2DDataValue()]; }
    @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return s.getValue(STANDING) ? POST : Shapes.empty(); }
}
