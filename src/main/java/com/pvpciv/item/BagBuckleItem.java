package com.pvpciv.item;

import com.pvpciv.data.StoryData;
import com.pvpciv.game.Story;
import com.pvpciv.inv.Bag;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Пряжка рюкзака: навсегда +3 слота сумки. */
public class BagBuckleItem extends Item {
    public final int slots;
    public BagBuckleItem(Properties p) { this(p, 3); }
    public BagBuckleItem(Properties p, int slots) { super(p); this.slots = slots; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            StoryData d = Story.data(sp);
            if (Bag.unlocked(d.level, d.extraSlots) >= Bag.MAX) {
                sp.displayClientMessage(Component.literal("Сумка уже максимального размера!").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(st);
            }
            d.extraSlots += slots;
            sp.setData(ModRegistry.STORY.get(), d);
            st.shrink(1);
            level.playSound(null, sp.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1f, 1.2f);
            Story.title(sp, "+" + slots + " слота сумки", "Теперь слотов: " + Bag.unlocked(d.level, d.extraSlots), 0x9EE7FF);
            Story.sync(sp);
        }
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("ПКМ — навсегда +" + slots + " слота сумки").withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal("Слоты также открываются на уровнях 4, 8, 12, 16, 20").withStyle(ChatFormatting.GRAY));
    }
}
