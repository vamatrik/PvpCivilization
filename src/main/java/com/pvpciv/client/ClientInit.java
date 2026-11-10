package com.pvpciv.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pvpciv.PvpCiv;
import com.pvpciv.net.Network;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class ClientInit {
    public static final KeyMapping DASH = new KeyMapping("key.pvpciv.dash", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.pvpciv");

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(DASH));
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) -> e.enqueueWork(() -> {
            for (var h : ModRegistry.ITEMS.getEntries())
                if (h.get() instanceof com.pvpciv.item.CivWeaponItem)
                    net.minecraft.client.renderer.item.ItemProperties.register(h.get(), PvpCiv.id("stars"),
                            (st, lvl, ent, seed) -> com.pvpciv.loot.Gear.stars(st) / 16f);
        }));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(ModRegistry.GUARD.get(), ctx -> new NpcRenderer<>(ctx, false, PvpCiv.id("textures/entity/guard.png")));
            e.registerEntityRenderer(ModRegistry.LUNA.get(), ctx -> new NpcRenderer<>(ctx, true, PvpCiv.id("textures/entity/luna.png")));
            e.registerEntityRenderer(ModRegistry.TRADER.get(), ctx -> new NpcRenderer<>(ctx, true, PvpCiv.id("textures/entity/trader.png")));
        });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAbove(VanillaGuiLayers.HOTBAR, PvpCiv.id("hud"), Hud::render));
        modBus.addListener((RegisterMenuScreensEvent e) -> {
            e.register(ModRegistry.INV_MENU.get(), com.pvpciv.client.screen.CivInventoryScreen::new);
            e.register(ModRegistry.FORGE_MENU.get(), com.pvpciv.client.screen.ForgeScreen::new);
            e.register(ModRegistry.DISMANTLE_MENU.get(), com.pvpciv.client.screen.DismantleScreen::new);
            e.register(ModRegistry.TRADER_MENU.get(), com.pvpciv.client.screen.TraderScreen::new);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Opening e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (e.getNewScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen && mc.player != null && !mc.player.isCreative()) {
                e.setCanceled(true);
                PacketDistributor.sendToServer(new Network.OpenInventory());
            }
            if (e.getNewScreen() instanceof net.minecraft.client.gui.screens.DeathScreen) e.setNewScreen(new com.pvpciv.client.screen.CivDeathScreen());
        });
        NeoForge.EVENT_BUS.addListener((CustomizeGuiOverlayEvent.BossEventProgress e) -> {
            e.setCanceled(true);
            e.setIncrement(Hud.bossBar(e.getGuiGraphics(), e.getBossEvent(), e.getX(), e.getY() + 6));
        });
        NeoForge.EVENT_BUS.addListener((CustomizeGuiOverlayEvent.Chat e) -> e.setPosY(e.getPosY() - 12));
        NeoForge.EVENT_BUS.addListener((RenderTooltipEvent.Color e) -> {
            var st = e.getItemStack();
            e.setBackground(0xF2100D18);
            if (st.getItem() instanceof com.pvpciv.item.CivWeaponItem w) {
                int c = (com.pvpciv.loot.Gear.rarity(st).ordinal() < w.minRarity.ordinal() ? w.minRarity : com.pvpciv.loot.Gear.rarity(st)).color;
                e.setBorderStart(0xFF000000 | c);
                e.setBorderEnd(0xFF000000 | ((c >> 1) & 0x7F7F7F));
            } else { e.setBorderStart(0xFFD9A441); e.setBorderEnd(0xFF7A5420); }
        });
        NeoForge.EVENT_BUS.addListener((ToastAddEvent e) -> {
            if (e.getToast() instanceof net.minecraft.client.gui.components.toasts.RecipeToast
                    || e.getToast() instanceof net.minecraft.client.gui.components.toasts.AdvancementToast
                    || e.getToast() instanceof net.minecraft.client.gui.components.toasts.TutorialToast) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((RenderGuiLayerEvent.Pre e) -> {
            var n = e.getName();
            if (n.equals(VanillaGuiLayers.HOTBAR) || n.equals(VanillaGuiLayers.PLAYER_HEALTH) || n.equals(VanillaGuiLayers.FOOD_LEVEL) || n.equals(VanillaGuiLayers.ARMOR_LEVEL)
                    || n.equals(VanillaGuiLayers.EXPERIENCE_BAR) || n.equals(VanillaGuiLayers.EXPERIENCE_LEVEL) || n.equals(VanillaGuiLayers.AIR_LEVEL) || n.equals(VanillaGuiLayers.VEHICLE_HEALTH))
                e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> onTick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> ClientState.reset());
    }

    private static void onTick() {
        Minecraft mc = Minecraft.getInstance();
        while (DASH.consumeClick()) {
            if (mc.player == null || mc.level == null) continue;
            long now = mc.level.getGameTime();
            if (mc.player.isPassenger() || now < ClientState.dashReadyAt) continue;
            ClientState.dashReadyAt = now + com.pvpciv.game.DashSkill.COOLDOWN_TICKS;
            PacketDistributor.sendToServer(new Network.Dash());
        }
    }
}
