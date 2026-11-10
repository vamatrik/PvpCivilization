package com.pvpciv.item;

import com.pvpciv.PvpCiv;
import com.pvpciv.loot.Gear;
import com.pvpciv.loot.Rarity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;

import java.util.List;

/** Оружие цивилизации: урон = база тира × редкость × заточка. */
public class CivWeaponItem extends SwordItem {
    public enum WClass {
        SWORD("Меч", 1.0, -2.4), SABER("Сабля", 0.85, -2.0), DAGGER("Кинжал", 0.65, -1.5),
        AXE("Топор", 1.3, -3.0), HAMMER("Молот", 1.5, -3.2);
        public final String title; public final double dmg; public final double speed;
        WClass(String t, double d, double s) { title = t; dmg = d; speed = s; }
    }

    public final int tier;
    public final WClass cls;
    public final String lore;
    public final Rarity minRarity;

    public CivWeaponItem(int tier, WClass cls, Rarity minRarity, String lore) {
        super(Tiers.IRON, new Item.Properties().stacksTo(1).component(DataComponents.UNBREAKABLE, new Unbreakable(false)));
        this.tier = tier; this.cls = cls; this.lore = lore; this.minRarity = minRarity;
    }

    public static double damage(int tier, WClass cls, Rarity r, int stars) {
        double starMul = stars >= 11 ? 2.2 : 1 + 0.1 * stars;
        return 4 * Math.pow(1.35, tier - 1) * cls.dmg * r.mult * starMul;
    }

    public double damage(ItemStack st) {
        Rarity r = Gear.rarity(st);
        if (r.ordinal() < minRarity.ordinal()) r = minRarity;
        return damage(tier, cls, r, Gear.stars(st));
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage(stack) - 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, cls.speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public Component getName(ItemStack st) {
        Rarity r = Gear.rarity(st);
        if (r.ordinal() < minRarity.ordinal()) r = minRarity;
        int s = Gear.stars(st);
        int rc = r.color;
        MutableComponent base = Component.translatable(getDescriptionId(st)).withStyle(x -> x.withColor(rc));
        if (s <= 0) return base;
        int sc = Gear.starColor(s);
        return Component.literal("+" + s + " ").withStyle(x -> x.withColor(sc).withBold(true)).append(base.withStyle(x -> x.withBold(false)));
    }

    @Override
    public boolean isFoil(ItemStack st) { return false; } // свечение заточки рисует сама модель

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        Rarity r = Gear.rarity(st);
        if (r.ordinal() < minRarity.ordinal()) r = minRarity;
        int rc = r.color;
        tip.add(Component.literal(r.title + " • " + cls.title + " • Тир " + tier).withStyle(x -> x.withColor(rc)));
        int s = Gear.stars(st);
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < Gear.MAX_STARS; i++) stars.append(i < s ? "★" : "☆");
        if (s >= 11) stars.append("✦");
        int sc = Gear.starColor(Math.max(1, s));
        tip.add(Component.literal(stars.toString()).withStyle(x -> x.withColor(s > 0 ? sc : 0x555555)));
        tip.add(Component.literal(String.format("Урон: %.1f", damage(st))).withStyle(ChatFormatting.GRAY));
        if (!lore.isEmpty()) tip.add(Component.literal(lore).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        if (s < Gear.MAX_STARS) tip.add(Component.literal(String.format("Заточка до +%d: %d%%", s + 1, (int) Math.round(Gear.chance(s) * 100))).withStyle(ChatFormatting.DARK_AQUA));
    }

    public static boolean isWorld1(ItemStack st) {
        return st.getItem() instanceof CivWeaponItem w && w.tier <= 10;
    }

    @SuppressWarnings("unused") private static final String NS = PvpCiv.MODID;
}
