package com.pvpciv.block;

import com.pvpciv.data.CivData;
import com.pvpciv.item.KeyCardItem;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

import java.util.UUID;

/** Железная дверь квартиры: открывается только ключ-картой жильца и сама закрывается через 3 секунды. */
public class ApartmentDoorBlock extends DoorBlock {
    public ApartmentDoorBlock(Properties props) { super(BlockSetType.IRON, props); }

    private static BlockPos lower(BlockState st, BlockPos pos) {
        return st.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModRegistry.KEY_CARD.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        BlockPos low = lower(state, pos);
        CivData data = CivData.get(level.getServer());
        CivData.Apartment apt = data.byDoor(low);
        UUID cardOwner = KeyCardItem.owner(stack);
        if (apt != null && !player.isCreative()) {
            if (!apt.npc.isEmpty()) {
                deny(level, pos, player, switch (apt.npc) {
                    case "luna" -> "«Эй! Это МОЯ дверь, извращенец!» — доносится изнутри.";
                    case "agafya" -> "Табличка: «Комендант принимает по вторникам. Сегодня не вторник. Никогда не вторник.»";
                    case "zero" -> "Ключ-карта не реагирует. За дверью тихо... слишком тихо.";
                    default -> "Не твоя квартира.";
                });
                return ItemInteractionResult.CONSUME;
            }
            if (apt.owner == null || cardOwner == null || !apt.owner.equals(cardOwner)) {
                deny(level, pos, player, "Доступ запрещён. Это не твоя квартира.");
                return ItemInteractionResult.CONSUME;
            }
        }
        BlockState lowState = level.getBlockState(low);
        if (!lowState.is(this)) return ItemInteractionResult.CONSUME;
        boolean open = !lowState.getValue(OPEN);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.6f, open ? 1.6f : 1.2f);
        this.setOpen(player, level, lowState, low, open);
        if (open) level.scheduleTick(low, this, 60);
        return ItemInteractionResult.CONSUME;
    }

    private void deny(Level level, BlockPos pos, Player player, String msg) {
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.8f, 0.5f);
        player.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.RED), true);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // изнутри квартиры дверь открывается без карты — выйти можно всегда
        BlockPos low = lower(state, pos);
        BlockState lowState = level.getBlockState(low);
        if (lowState.is(this)) {
            net.minecraft.core.Direction f = lowState.getValue(FACING);
            double dot = (player.getX() - (low.getX() + 0.5)) * f.getStepX() + (player.getZ() - (low.getZ() + 0.5)) * f.getStepZ();
            if (dot < -0.4) {
                if (!level.isClientSide && !lowState.getValue(OPEN)) {
                    level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.6f, 1.6f);
                    this.setOpen(player, level, lowState, low, true);
                    level.scheduleTick(low, this, 60);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        if (!level.isClientSide) {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.6f, 0.6f);
            player.displayClientMessage(Component.literal("🔒 Нужна ключ-карта (ПКМ картой по двери)").withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.is(this) && state.getValue(HALF) == DoubleBlockHalf.LOWER && state.getValue(OPEN)) {
            this.setOpen(null, level, state, pos, false);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block block, BlockPos from, boolean moving) {
        // игнорируем редстоун — только ключ-карта
    }
}
