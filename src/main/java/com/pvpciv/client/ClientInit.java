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
        modBus.addListener((net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional e) -> e.register(StashRenderer.LID));
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) -> e.enqueueWork(() -> {
            for (var h : ModRegistry.ITEMS.getEntries())
                if (h.get() instanceof com.pvpciv.item.CivWeaponItem)
                    net.minecraft.client.renderer.item.ItemProperties.register(h.get(), PvpCiv.id("stars"),
                            (st, lvl, ent, seed) -> com.pvpciv.loot.Gear.stars(st) / 16f);
        }));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(ModRegistry.GUARD.get(), ctx -> new NpcRenderer<>(ctx, false, PvpCiv.id("textures/entity/guard.png")));
            e.registerEntityRenderer(ModRegistry.LUNA.get(), GeoNpcRenderer::new);
            e.registerEntityRenderer(ModRegistry.LUNA_GUIDE.get(), GeoNpcRenderer::new);
            e.registerEntityRenderer(ModRegistry.TIMOFEY.get(), ctx -> new NpcRenderer<>(ctx, false, PvpCiv.id("textures/entity/timofey.png")));
            e.registerEntityRenderer(ModRegistry.PARACHUTIST.get(), ctx -> new NpcRenderer<>(ctx, true, PvpCiv.id("textures/entity/parachutist.png")));
            e.registerEntityRenderer(ModRegistry.PLUMBER.get(), ctx -> new NpcRenderer<>(ctx, false, PvpCiv.id("textures/entity/plumber.png")));
            e.registerEntityRenderer(ModRegistry.SMUGGLER.get(), ctx -> new NpcRenderer<>(ctx, false, PvpCiv.id("textures/entity/smuggler.png")));
            e.registerEntityRenderer(ModRegistry.CARMILLA.get(), GeoNpcRenderer::new);
            e.registerEntityRenderer(ModRegistry.ELEONORA.get(), GeoNpcRenderer::new);
            MobRenderers.register(e);
            e.registerEntityRenderer(com.pvpciv.registry.ModMobs.BRUTE_BOSS.get(), ctx -> new BossRenderer<>(ctx, "brute", 1f, 1.3f));
            e.registerEntityRenderer(com.pvpciv.registry.ModMobs.KEEPER_BOSS.get(), ctx -> new BossRenderer<>(ctx, "keeper", 1.3f, 0.6f));
            e.registerEntityRenderer(com.pvpciv.registry.ModMobs.DON_BOSS.get(), ctx -> new BossRenderer<>(ctx, "don", 1.25f, 0.6f));
            e.registerEntityRenderer(com.pvpciv.registry.ModMobs.RAT_KING_BOSS.get(), ctx -> new BossRenderer<>(ctx, "rat_king", 1.35f, 0.8f));
            e.registerBlockEntityRenderer(ModRegistry.STASH_BE.get(), ctx -> new StashRenderer());
            e.registerBlockEntityRenderer(ModRegistry.PEDESTAL_BE.get(), ctx -> new PedestalRenderer());
            e.registerBlockEntityRenderer(ModRegistry.SHOWCASE_BE.get(), ctx -> new ShowcaseRenderer());
            e.registerEntityRenderer(ModRegistry.TRADER.get(), ctx -> new NpcRenderer<>(ctx, true, PvpCiv.id("textures/entity/trader.png")));
        });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAbove(VanillaGuiLayers.HOTBAR, PvpCiv.id("hud"), Hud::render));
        modBus.addListener((RegisterMenuScreensEvent e) -> {
            e.register(ModRegistry.INV_MENU.get(), com.pvpciv.client.screen.CivInventoryScreen::new);
            e.register(ModRegistry.FORGE_MENU.get(), com.pvpciv.client.screen.ForgeScreen::new);
            e.register(ModRegistry.DISMANTLE_MENU.get(), com.pvpciv.client.screen.DismantleScreen::new);
            e.register(ModRegistry.INFUSION_MENU.get(), com.pvpciv.client.screen.InfusionScreen::new);
            e.register(ModRegistry.ARMOR_MENU.get(), com.pvpciv.client.screen.ArmorBenchScreen::new);
            e.register(ModRegistry.HOME_CHEST_MENU.get(), com.pvpciv.client.screen.HomeChestScreen::new);
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
            if (st.getItem() instanceof com.pvpciv.item.CivArmorItem) {
                int c = com.pvpciv.loot.Gear.rarity(st).color;
                e.setBorderStart(0xFF000000 | c);
                e.setBorderEnd(0xFF000000 | ((c >> 1) & 0x7F7F7F));
            } else if (st.getItem() instanceof com.pvpciv.item.CivWeaponItem w) {
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

    /** Лодки выше тира 1 и титул «Ледяной гонщик» — меньше трения на льду (лодкой управляет клиент). */
    private static void boatBoost(Minecraft mc) {
        if (mc.player == null || !(mc.player.getVehicle() instanceof net.minecraft.world.entity.vehicle.Boat b) || b.getControllingPassenger() != mc.player) return;
        double bonus = b.getVariant() == net.minecraft.world.entity.vehicle.Boat.Type.BIRCH ? com.pvpciv.item.BoatSummonItem.speedBonus(2) : 0;
        if ("ice_racer".equals(ClientState.activeTitle)) bonus += 0.06;
        if (bonus <= 0 || !b.onGround()) return;
        var v = b.getDeltaMovement();
        if (v.x * v.x + v.z * v.z < 0.0025) return;
        double k = 1 + 0.012 * bonus / 0.1;
        b.setDeltaMovement(v.x * k, v.y, v.z * k);
    }

    private static void onTick() {
        Minecraft mc = Minecraft.getInstance();
        boatBoost(mc);
        while (DASH.consumeClick()) {
            if (mc.player == null || mc.level == null) continue;
            long now = mc.level.getGameTime();
            if (mc.player.isPassenger() || now < ClientState.dashReadyAt) continue;
            ClientState.dashReadyAt = now + com.pvpciv.game.DashSkill.COOLDOWN_TICKS;
            PacketDistributor.sendToServer(new Network.Dash());
        }
    }
}
