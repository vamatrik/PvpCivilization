package com.pvpciv.game;

import com.pvpciv.floor.Floor1;
import com.pvpciv.floor.Floor2;
import com.pvpciv.inv.Bag;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.CivWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Падение с этажа 2 — на этаж 1 с неба. Парашют (в сумке или инвентаре) раскрывается сам при долгом падении. */
public final class Falling {
    public static void tick(ServerPlayer p) {
        if (p.isCreative() || p.isSpectator()) return;
        if (CivWorld.isF2(p.level()) && p.getY() < Floor2.YS - 16) {
            double a = p.getRandom().nextDouble() * Math.PI * 2, r = 8 + p.getRandom().nextDouble() * 18;
            double x = Floor1.CAMP.getX() + 0.5 + Math.cos(a) * r, z = Floor1.CAMP.getZ() + 40 + Math.sin(a) * r;
            p.teleportTo(CivWorld.level(p.server), x, Floor1.Y + 150, z, p.getYRot(), 30);
            p.resetFallDistance();
            p.sendSystemMessage(Component.literal("Ты сорвался с края Неонового города и летишь вниз, на этаж 1!").withStyle(ChatFormatting.RED));
            return;
        }
        var tag = p.getPersistentData();
        if (p.onGround() || p.isInWater() || p.isPassenger() || p.getAbilities().flying) { tag.remove("chute"); return; }
        boolean open = tag.getBoolean("chute");
        if (!open && p.fallDistance > 10 && hasChute(p)) {
            tag.putBoolean("chute", true);
            open = true;
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA.value(), SoundSource.PLAYERS, 1f, 0.8f);
            p.serverLevel().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 2.5, p.getZ(), 20, 0.8, 0.2, 0.8, 0.01);
            p.displayClientMessage(Component.literal("⛱ Парашют раскрылся!").withStyle(ChatFormatting.GOLD), true);
        }
        if (open) {
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, false, false));
            p.resetFallDistance();
        }
    }

    private static boolean hasChute(ServerPlayer p) {
        return Bag.has(p, s -> s.is(ModRegistry.PARACHUTE.get()));
    }
}
