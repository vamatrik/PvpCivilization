package com.pvpciv.block;

import com.mojang.serialization.MapCodec;
import com.pvpciv.game.Showcases;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Настенная витрина трофеев этажа (1.1). STYLE — этаж (1..5), PART — кусок секции шириной 1–3 блока. FACING — лицом в комнату. */
public class TrophyShelfBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public enum Part implements StringRepresentable {
        SINGLE, LEFT, MIDDLE, RIGHT;
        @Override public String getSerializedName() { return name().toLowerCase(); }
    }
    public static final IntegerProperty STYLE = IntegerProperty.create("style", 1, 5);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final MapCodec<TrophyShelfBlock> CODEC = simpleCodec(TrophyShelfBlock::new);

    public TrophyShelfBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STYLE, 1).setValue(PART, Part.SINGLE));
    }

    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, STYLE, PART); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState st) { return new TrophyShelfBlockEntity(pos, st); }

    private static final VoxelShape[] SHAPES = new VoxelShape[4];
    static {
        // модель смотрит на север: задняя стенка у z=16
        for (Direction d : Direction.Plane.HORIZONTAL) {
            VoxelShape s = switch (d) {
                case NORTH -> Block.box(0, 0, 2, 16, 16, 16);
                case SOUTH -> Block.box(0, 0, 0, 16, 16, 14);
                case EAST -> Block.box(0, 0, 0, 14, 16, 16);
                default -> Block.box(2, 0, 0, 16, 16, 16);
            };
            SHAPES[d.get2DDataValue()] = s;
        }
    }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return SHAPES[s.getValue(FACING).get2DDataValue()]; }
    @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return Shapes.empty(); }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack st, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int bit = Showcases.bitOf(st.getItem());
        if (bit == 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) Showcases.place(sp, pos, st, bit);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TrophyShelfBlockEntity be) {
            String name = Showcases.name(be.bit);
            player.displayClientMessage(Component.literal(name == null ? "Витрина этажа " + state.getValue(STYLE) + "." :
                    (be.shown ? "🏆 " + name : "Место для трофея: " + name + " · ПКМ трофеем — выставить")).withStyle(be.shown ? ChatFormatting.GOLD : ChatFormatting.GRAY), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
