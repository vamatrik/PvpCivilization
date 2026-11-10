package com.pvpciv.item;

import com.pvpciv.PvpCiv;
import com.pvpciv.loot.Gear;
import com.pvpciv.loot.Rarity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;

import java.util.List;

/**
 * Броня цивилизации: броня и здоровье = база тира × редкость. Не ломается.
 * Позже: опыт брони из материалов и свитки-пороги на верстаке брони.
 */
public class CivArmorItem extends ArmorItem {
    public final double tier;
    public final String set;
    public final String lore;

    public CivArmorItem(Holder<ArmorMaterial> mat, Type type, double tier, String set, String lore) {
        super(mat, type, new Item.Properties().stacksTo(1).component(DataComponents.UNBREAKABLE, new Unbreakable(false)));
        this.tier = tier; this.set = set; this.lore = lore;
    }

    private double part() {
        return switch (type) { case HELMET -> 0.18; case CHESTPLATE -> 0.36; case LEGGINGS -> 0.28; case BOOTS -> 0.18; default -> 0.2; };
    }

    /** Броня (очки) и бонус к здоровью за предмет. Полный комплект тира 1+ обычной редкости ≈ 8 брони и +5 HP. */
    public double armor(ItemStack st) { return 8 * Math.pow(1.25, tier - 1.5) * part() * Gear.rarity(st).mult; }
    public double health(ItemStack st) { return 5 * Math.pow(1.3, tier - 1.5) * part() * Gear.rarity(st).mult; }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack st) {
        EquipmentSlotGroup g = EquipmentSlotGroup.bySlot(type.getSlot());
        ResourceLocation id = PvpCiv.id("armor." + type.getName());
        return ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(id, armor(st), AttributeModifier.Operation.ADD_VALUE), g)
                .add(Attributes.MAX_HEALTH, new AttributeModifier(id, health(st), AttributeModifier.Operation.ADD_VALUE), g)
                .build();
    }

    @Override
    public Component getName(ItemStack st) {
        int rc = Gear.rarity(st).color;
        MutableComponent base = Component.translatable(getDescriptionId(st)).withStyle(x -> x.withColor(rc));
        return base;
    }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        Rarity r = Gear.rarity(st);
        int rc = r.color;
        String t = tier == Math.floor(tier) ? String.valueOf((int) tier) : (int) Math.floor(tier) + "+";
        tip.add(Component.literal(r.title + " • " + set + " • Тир " + t).withStyle(x -> x.withColor(rc)));
        tip.add(Component.literal(String.format("Броня: +%.1f   Здоровье: +%.1f", armor(st), health(st))).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Верстак брони (скоро): опыт из материалов").withStyle(ChatFormatting.DARK_AQUA));
        if (!lore.isEmpty()) tip.add(Component.literal(lore).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override public boolean isFoil(ItemStack st) { return Gear.rarity(st).ordinal() >= Rarity.LEGENDARY.ordinal(); }
}
