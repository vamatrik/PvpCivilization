package com.pvpciv.block;

import com.pvpciv.menu.ArmorBenchMenu;
import com.pvpciv.menu.HomeChestMenu;
import com.pvpciv.menu.InfusionMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Станции 0.7: алтарь инфузии, пьедестал (декор круга), стол брони, домашний сундук. */
public class CivStationBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
    public enum Kind { ALTAR, PEDESTAL, ARMOR, HOME_CHEST, SHOWCASE, LIFT }
    private static final VoxelShape PEDESTAL_SHAPE = Block.box(3, 0, 3, 13, 14, 13), ALTAR_SHAPE = Block.box(0, 0, 0, 16, 12, 16),
            CHEST_SHAPE = Block.box(1, 0, 1, 15, 14, 15);
    public final Kind kind;
    private final boolean facing;

    public CivStationBlock(Properties p, boolean facing, Kind kind) {
        super(p);
        this.facing = facing;
        this.kind = kind;
        if (facing) registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        // FACING есть у всех экземпляров класса — у «безликих» оно просто не используется в blockstate-файле
        b.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing ? ctx.getHorizontalDirection().getOpposite() : Direction.NORTH);
    }

    @Override
    protected BlockState rotate(BlockState s, Rotation r) {
        return facing ? s.setValue(HorizontalDirectionalBlock.FACING, r.rotate(s.getValue(HorizontalDirectionalBlock.FACING))) : s;
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return switch (kind) { case PEDESTAL -> PEDESTAL_SHAPE; case ALTAR -> ALTAR_SHAPE; case HOME_CHEST -> CHEST_SHAPE; default -> super.getShape(s, g, p, c); };
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState st) {
        if (kind == Kind.SHOWCASE) return new ShowcaseBlockEntity(pos, st);
        return kind == Kind.ALTAR || kind == Kind.PEDESTAL ? new PedestalBlockEntity(pos, st) : null;
    }

    /** Что можно положить: на пьедестал — звёзды, на алтарь — оружие от +5 или Медальон инея. */
    public static boolean accepts(Kind k, net.minecraft.world.item.ItemStack st) {
        if (k == Kind.PEDESTAL) return st.is(com.pvpciv.registry.ModRegistry.STAR_W1.get()) || st.is(com.pvpciv.registry.ModRegistry.BLESSED_STAR_W1.get());
        return (st.getItem() instanceof com.pvpciv.item.CivWeaponItem && com.pvpciv.loot.Gear.stars(st) >= 5) || st.is(com.pvpciv.registry.ModRegistry.FROZEN_LOCKET.get()) || st.is(com.pvpciv.registry.ModRegistry.GAS_MASK_FILTER.get());
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack st, BlockState state, Level level, BlockPos pos, Player player,
                                                                  net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (kind == Kind.SHOWCASE) {
            int bit = ShowcaseBlockEntity.bitOf(st.getItem());
            if (bit == 0) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (level.isClientSide) return net.minecraft.world.ItemInteractionResult.SUCCESS;
            if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof ShowcaseBlockEntity be)) return net.minecraft.world.ItemInteractionResult.FAIL;
            if (!com.pvpciv.game.HomeChest.canUse(sp, pos)) { sp.displayClientMessage(Component.literal("Это чужая витрина.").withStyle(ChatFormatting.RED), true); return net.minecraft.world.ItemInteractionResult.CONSUME; }
            var d = com.pvpciv.progress.Progress.d(sp);
            if ((d.showcase & bit) != 0) { sp.displayClientMessage(Component.literal("Этот трофей уже стоит на витрине.").withStyle(ChatFormatting.GRAY), true); return net.minecraft.world.ItemInteractionResult.CONSUME; }
            d.showcase |= bit;
            sp.setData(com.pvpciv.registry.ModRegistry.STORY.get(), d);
            be.add(bit);
            st.shrink(1);
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.BLOCKS, 0.7f, 1.4f);
            sp.sendSystemMessage(Component.literal("🏆 Трофей выставлен на витрину!").withStyle(ChatFormatting.GOLD));
            com.pvpciv.game.Story.sync(sp);
            return net.minecraft.world.ItemInteractionResult.CONSUME;
        }
        if (kind != Kind.ALTAR && kind != Kind.PEDESTAL) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!accepts(kind, st)) return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return net.minecraft.world.ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof PedestalBlockEntity be)) return net.minecraft.world.ItemInteractionResult.FAIL;
        if (!be.item().isEmpty()) {
            player.displayClientMessage(Component.literal("Здесь уже лежит предмет" + (be.ownerName().isEmpty() ? "" : " (" + be.ownerName() + ")")).withStyle(ChatFormatting.GRAY), true);
            return net.minecraft.world.ItemInteractionResult.CONSUME;
        }
        be.set(st.split(1), player.getUUID(), player.getGameProfile().getName());
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 1f, kind == Kind.ALTAR ? 0.7f : 1.2f);
        if (level instanceof net.minecraft.server.level.ServerLevel sl)
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 15, 0.2, 0.3, 0.2, 0.4);
        return net.minecraft.world.ItemInteractionResult.CONSUME;
    }

    private static boolean takeBack(ServerPlayer sp, PedestalBlockEntity be) {
        if (be.item().isEmpty()) return false;
        if (be.owner() != null && !be.owner().equals(sp.getUUID())) {
            sp.displayClientMessage(Component.literal("Это положил(а) " + be.ownerName() + " — не трогай.").withStyle(ChatFormatting.RED), true);
            return true;
        }
        com.pvpciv.inv.Bag.give(sp, be.item().copy());
        be.set(net.minecraft.world.item.ItemStack.EMPTY, null, "");
        sp.level().playSound(null, be.getBlockPos(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 1f);
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResult.sidedSuccess(level.isClientSide);
        switch (kind) {
            case ALTAR -> {
                if (level.getBlockEntity(pos) instanceof PedestalBlockEntity be && (sp.isShiftKeyDown() || be.item().getItem() instanceof com.pvpciv.item.ArmorCoreItem
                        || be.item().is(com.pvpciv.registry.ModRegistry.BLESSED_STAR_W1.get())) && takeBack(sp, be)) break;   // результат ритуала — просто ПКМ
                sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new InfusionMenu(id, inv, pos), Component.literal("Алтарь инфузии")), b -> b.writeBlockPos(pos));
            }
            case ARMOR -> sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ArmorBenchMenu(id, inv), Component.literal("Стол брони")));
            case HOME_CHEST -> {
                if (!com.pvpciv.game.HomeChest.canUse(sp, pos)) {
                    sp.displayClientMessage(Component.literal("Это чужой сундук.").withStyle(ChatFormatting.RED), true);
                    break;
                }
                sp.openMenu(new SimpleMenuProvider((id, inv, pl) -> new HomeChestMenu(id, inv), Component.literal("Домашний сундук")));
            }
            case LIFT -> com.pvpciv.game.SkyLift.use(sp);
            case SHOWCASE -> {
                int n = level.getBlockEntity(pos) instanceof ShowcaseBlockEntity be ? Integer.bitCount(be.bits) : 0;
                sp.displayClientMessage(Component.literal("Витрина трофеев: " + n + "/4. ПКМ трофеем — выставить.").withStyle(ChatFormatting.GOLD), true);
            }
            case PEDESTAL -> {
                if (level.getBlockEntity(pos) instanceof PedestalBlockEntity be && takeBack(sp, be)) break;
                sp.displayClientMessage(Component.literal("Положи сюда звезду (ПКМ звездой). Ритуал — ПКМ по алтарю.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
        }
        return InteractionResult.CONSUME;
    }
}
