package com.pvpciv.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerDebugMixin {
    @Inject(method = "resetAttackStrengthTicker", at = @At("HEAD"))
    private void pvpciv$dbg(CallbackInfo ci) {
        if (com.pvpciv.combat.CombatEvents.DMGLOG && !((Player) (Object) this).level().isClientSide) new Throwable("RESETLOG").printStackTrace(System.out);
    }
}
