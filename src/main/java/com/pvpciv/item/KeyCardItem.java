package com.pvpciv.item;

import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class KeyCardItem extends Item {
    public KeyCardItem(Properties props) { super(props); }

    public static ItemStack create(Player owner, int apartmentNumber) {
        ItemStack st = new ItemStack(ModRegistry.KEY_CARD.get());
        CompoundTag t = new CompoundTag();
        t.putUUID("owner", owner.getUUID());
        t.putString("ownerName", owner.getGameProfile().getName());
        t.putInt("apt", apartmentNumber);
        st.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        st.set(DataComponents.CUSTOM_NAME, Component.literal("Ключ-карта: " + owner.getGameProfile().getName())
                .withStyle(s -> s.withColor(0x7FDBFF).withItalic(false)));
        return st;
    }

    @Nullable
    public static UUID owner(ItemStack st) {
        CustomData cd = st.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        CompoundTag t = cd.copyTag();
        return t.hasUUID("owner") ? t.getUUID("owner") : null;
    }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        CustomData cd = st.get(DataComponents.CUSTOM_DATA);
        if (cd != null) {
            CompoundTag t = cd.copyTag();
            tip.add(Component.literal("Жилец: " + t.getString("ownerName")).withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("Квартира №" + t.getInt("apt")).withStyle(ChatFormatting.GRAY));
        }
        tip.add(Component.literal("ПКМ по двери — открыть").withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.literal("«Потеряешь — будешь жить в коридоре, как Гриша.»").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
