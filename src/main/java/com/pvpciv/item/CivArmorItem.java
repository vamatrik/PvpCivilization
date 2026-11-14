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
    public double armor(ItemStack st) { return com.pvpciv.combat.Balance.armorDef(tier) * part() * Gear.rarity(st).mult * com.pvpciv.game.ArmorUp.mult(Gear.stars(st)); }
    public double health(ItemStack st) { return com.pvpciv.combat.Balance.armorHp(tier) * part() * Gear.rarity(st).mult * com.pvpciv.game.ArmorUp.mult(Gear.stars(st)); }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack st) {
        EquipmentSlotGroup g = EquipmentSlotGroup.bySlot(type.getSlot());
        ResourceLocation id = PvpCiv.id("armor." + type.getName());
        return ItemAttributeModifiers.builder()
                .add(Attributes.MAX_HEALTH, new AttributeModifier(id, health(st), AttributeModifier.Operation.ADD_VALUE), g)
                .build();
    }

    @Override
    public Component getName(ItemStack st) {
        int rc = Gear.rarity(st).color;
        MutableComponent base = Component.translatable(getDescriptionId(st)).withStyle(x -> x.withColor(rc));
        int lv = Gear.stars(st);
        if (lv > 0) base = Component.literal("+" + lv + " ").withStyle(x -> x.withColor(Gear.starColor(lv))).append(base);
        return base;
    }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        Rarity r = Gear.rarity(st);
        int rc = r.color;
        String t = tier == Math.floor(tier) ? String.valueOf((int) tier) : (int) Math.floor(tier) + "+";
        tip.add(Component.literal(r.title + " • " + set + " • Тир " + t).withStyle(x -> x.withColor(rc)));
        tip.add(Component.literal(String.format("Броня: +%.1f   Здоровье: +%.1f", armor(st), health(st))).withStyle(ChatFormatting.GRAY));
        if (set.startsWith("Самурай")) tip.add(Component.literal("Полный комплект: +5% шанс крита").withStyle(ChatFormatting.DARK_AQUA));
        if (set.startsWith("Экзокостюм")) tip.add(Component.literal("Полный комплект: −10% урона от пуль и плазмы").withStyle(ChatFormatting.DARK_AQUA));
        if (set.startsWith("Лисий")) tip.add(Component.literal("Полный комплект: прыжок и скорость").withStyle(ChatFormatting.DARK_AQUA));
        int lv = Gear.stars(st);
        if (lv >= com.pvpciv.game.ArmorUp.MAX) tip.add(Component.literal("Улучшение: +11 (максимум)").withStyle(ChatFormatting.DARK_RED));
        else {
            int need = com.pvpciv.game.ArmorUp.need(Math.min(lv, 10)), xp = com.pvpciv.game.ArmorUp.xp(st);
            int fill = (int) Math.round(10.0 * xp / need);
            tip.add(Component.literal("Улучшение +" + lv + "  ").withStyle(ChatFormatting.DARK_AQUA)
                    .append(Component.literal("■".repeat(fill)).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("■".repeat(10 - fill)).withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("  " + xp + "/" + need).withStyle(ChatFormatting.GRAY)));
            tip.add(Component.literal("Стол брони в квартире: хлам → опыт, ядро → +1").withStyle(ChatFormatting.DARK_GRAY));
        }
        com.pvpciv.game.Enchanting.tip(st, tip);
        if (!lore.isEmpty()) tip.add(Component.literal(lore).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override public boolean isFoil(ItemStack st) { return Gear.rarity(st).ordinal() >= Rarity.LEGENDARY.ordinal(); }
}
