package com.pvpciv;

import com.pvpciv.game.GameEvents;
import com.pvpciv.net.Network;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(PvpCiv.MODID)
public class PvpCiv {
    public static final String MODID = "pvpciv";

    public PvpCiv(IEventBus modBus) {
        ModRegistry.register(modBus);
        modBus.addListener(ModRegistry::onAttributes);
        modBus.addListener(Network::register);
        GameEvents.register(NeoForge.EVENT_BUS);
        if (FMLEnvironment.dist.isClient()) {
            com.pvpciv.client.ClientInit.init(modBus);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
