package com.pvpciv.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.2: элитные мобы — без ванильного контура свечения (вместо него своя 3D-аура, см. EliteAura). */
@Mixin(Minecraft.class)
public abstract class MinecraftGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void pvpciv$noEliteOutline(Entity e, CallbackInfoReturnable<Boolean> cir) {
        if (com.pvpciv.client.EliteAura.isElite(e)) cir.setReturnValue(false);
    }
}
