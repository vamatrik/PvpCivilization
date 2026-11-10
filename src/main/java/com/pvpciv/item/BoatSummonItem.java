package com.pvpciv.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;
import java.util.UUID;

/** Лодка-спавнер: ПКМ по блоку ставит лодку, повторный ПКМ рядом убирает. Потерять нельзя. */
public class BoatSummonItem extends Item {
    public static final String OWNER_TAG = "pvpciv_owner";

    public BoatSummonItem(Properties p) { super(p); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        ItemStack st = ctx.getItemInHand();
        CompoundTag t = st.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (t.hasUUID("boat")) {
            Entity old = level.getEntity(t.getUUID("boat"));
            if (old instanceof Boat b && b.isAlive()) {
                boolean near = b.distanceTo(p) < 8;
                b.ejectPassengers();
                b.discard();
                level.playSound(null, b.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8f, 0.8f);
                if (near) {
                    p.displayClientMessage(Component.literal("Лодка убрана").withStyle(ChatFormatting.AQUA), true);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        Boat boat = new Boat(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        boat.setVariant(Boat.Type.OAK);
        boat.setYRot(p.getYRot());
        if (!level.noCollision(boat, boat.getBoundingBox())) {
            p.displayClientMessage(Component.literal("Тут не поместится").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        UUID id = boat.getUUID();
        boat.getPersistentData().putUUID(OWNER_TAG, p.getUUID());
        level.addFreshEntity(boat);
        t.putUUID("boat", id);
        st.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        level.playSound(null, pos, SoundEvents.BOAT_PADDLE_WATER, SoundSource.PLAYERS, 1f, 1.2f);
        p.displayClientMessage(Component.literal("Лодка на льду! ПКМ по блоку рядом — убрать").withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Тир 1 • Дубовая • Обычная").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("ПКМ по льду — поставить лодку").withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.literal("ПКМ рядом с лодкой — убрать обратно").withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.literal("Далеко? ПКМ — лодка вернётся к тебе").withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.literal("«Это не лодка. Это ДУША на льду.» — Ледоруб").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
