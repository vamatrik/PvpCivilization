package com.pvpciv.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import java.util.List;

/** Зелья охотницы Элеоноры: пьются как обычные, бутылка не остаётся. Своя текстура у каждого. */
public class CivPotionItem extends Item {
    public enum Kind {
        HEAL("Мгновенно лечит 35% здоровья."), REGEN("Регенерация II, 10 с."), SWIFT("Скорость I, 90 с."),
        MIGHT("Сила I, 60 с."), STONE("Сопротивление урону I, 45 с.");
        public final String desc;
        Kind(String d) { desc = d; }
    }

    public final Kind kind;

    public CivPotionItem(Properties p, Kind kind) { super(p.rarity(Rarity.UNCOMMON)); this.kind = kind; }

    @Override public int getUseDuration(ItemStack st, LivingEntity e) { return 24; }
    @Override public UseAnim getUseAnimation(ItemStack st) { return UseAnim.DRINK; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
        if (kind == Kind.HEAL && p.getHealth() >= p.getMaxHealth()) return InteractionResultHolder.fail(p.getItemInHand(h));
        return ItemUtils.startUsingInstantly(l, p, h);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack st, Level l, LivingEntity e) {
        if (!l.isClientSide) {
            switch (kind) {
                case HEAL -> e.heal(e.getMaxHealth() * 0.35f);
                case REGEN -> e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                case SWIFT -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1800, 0));
                case MIGHT -> e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0));
                case STONE -> e.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 900, 0));
            }
            l.playSound(null, e.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.4f);
        }
        if (!(e instanceof Player pl) || !pl.getAbilities().instabuild) st.shrink(1);
        return st;
    }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, TooltipFlag f) {
        tip.add(Component.literal(kind.desc).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Зелье охотницы Элеоноры.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
