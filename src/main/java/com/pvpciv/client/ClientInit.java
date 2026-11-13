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
    /** Навыки 2 и 3 (F занята второй рукой): G и V — рядом с WASD и свободны в ванилле. Телефон — Y. */
    public static final KeyMapping SKILL2 = new KeyMapping("key.pvpciv.skill2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.pvpciv");
    public static final KeyMapping SKILL3 = new KeyMapping("key.pvpciv.skill3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.pvpciv");
    public static final KeyMapping PHONE = new KeyMapping("key.pvpciv.phone", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Y, "key.categories.pvpciv");
    public static final KeyMapping[] SKILL_KEYS = {DASH, SKILL2, SKILL3};

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> { e.register(DASH); e.register(SKILL2); e.register(SKILL3); e.register(PHONE); });
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
            e.registerEntityRenderer(ModRegistry.CIV_BOAT.get(), CivBoatRenderer::new);
            e.registerEntityRenderer(ModRegistry.LIFT_CABIN.get(), LiftCabinRenderer::new);
            e.registerEntityRenderer(ModRegistry.TIMOFEY.get(), ctx -> new NpcRenderer<>(ctx, false, PvpCiv.id("textures/entity/timofey.png")));
            e.registerEntityRenderer(ModRegistry.PARACHUTIST.get(), ctx -> new NpcRenderer<>(ctx, true, PvpCiv.id("textures/entity/parachutist.png")));
            e.registerEntityRenderer(ModRegistry.PLUMBER.get(), ctx -> new NpcRenderer<>(ctx, false, PvpCiv.id("textures/entity/plumber.png")));
            e.registerEntityRenderer(ModRegistry.COURIER.get(), CourierRenderer::new);
            e.registerEntityRenderer(ModRegistry.GATE.get(), GateRenderer::new);
            e.registerEntityRenderer(ModRegistry.FX.get(), FxRenderer::new);
            e.registerEntityRenderer(ModRegistry.LIBRARIAN.get(), GeoNpcRenderer::new);
            e.registerEntityRenderer(ModRegistry.CHEF.get(), GeoNpcRenderer::new);
            e.registerEntityRenderer(ModRegistry.ASSASSIN.get(), GeoNpcRenderer::new);
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
            e.registerEntityRenderer(ModRegistry.TRADER.get(), SorokaRenderer::new);
        });
        modBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers e) -> {
            for (var skin : e.getSkins()) {
                var r = e.getSkin(skin);
                if (r instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer pr) pr.addLayer(new GasMaskLayer(pr));
            }
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
        // поездка на лифте: камера отъезжает — смотрим на кабину издалека, от 3-го лица, с медленным облётом (бета 12.11)
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent e) -> {
            if (liftCam()) e.setDistance(14f);
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles e) -> {
            if (!liftCam()) return;
            float t = (System.currentTimeMillis() - ClientState.cineStart) / 1000f;
            e.setYaw(camYaw + 40 + t * 9f);
            e.setPitch(12f);
        });
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

    /**
     * Лодки выше тира 1 (лодкой управляет клиент): быстрее по льду и по земле (отдельные параметры), доворачивают быстрее
     * и меньше «скользят» — скорость поворачивается за носом лодки. Титул «Ледяной гонщик» — ещё +6% на льду.
     */
    private static void boatBoost(Minecraft mc) {
        if (mc.player == null || !(mc.player.getVehicle() instanceof net.minecraft.world.entity.vehicle.Boat b) || b.getControllingPassenger() != mc.player) return;
        int tier = b instanceof com.pvpciv.entity.CivBoat cb ? cb.tier() : b.getVariant() == net.minecraft.world.entity.vehicle.Boat.Type.BIRCH ? 2 : 1;
        var in = mc.player.input;
        float turn = com.pvpciv.item.BoatSummonItem.turnBonus(tier);
        // бета 0.8.7: «странно управляется» — ванильная лодка копит вращение и крутится после отпускания клавиши.
        // Гасим занос: без руля вращение быстро затухает, с рулём — ограничено.
        Float rot = spin(b);
        if (rot != null) {
            float lim = 5f + turn;
            float nr = in.left == in.right ? rot * 0.45f : Math.max(-lim, Math.min(lim, rot));
            if (nr != rot) spin(b, nr);
        }
        if (turn > 0 && in.left != in.right) {
            float d = in.left ? -turn : turn;
            b.setYRot(b.getYRot() + d);
            mc.player.setYRot(mc.player.getYRot() + d);
        }
        if (!b.onGround()) return;
        var below = mc.level.getBlockState(b.blockPosition().below());
        boolean ice = below.getBlock().getFriction() > 0.9f || mc.level.getBlockState(b.blockPosition()).getBlock().getFriction() > 0.9f;
        double bonus = ice ? com.pvpciv.item.BoatSummonItem.iceBonus(tier) : com.pvpciv.item.BoatSummonItem.landBonus(tier);
        if (ice && "ice_racer".equals(ClientState.activeTitle)) bonus += 0.06;
        var v = b.getDeltaMovement();
        double sp2 = v.x * v.x + v.z * v.z;
        if (sp2 < 0.0025) return;
        // сцепление: часть скорости разворачивается по направлению носа
        double grip = com.pvpciv.item.BoatSummonItem.grip(tier), sp = Math.sqrt(sp2);
        if (grip > 0) {
            double yaw = Math.toRadians(b.getYRot()), fx = -Math.sin(yaw), fz = Math.cos(yaw);
            double dir = fx * v.x + fz * v.z >= 0 ? 1 : -1;   // задний ход — разворачиваем назад
            v = new net.minecraft.world.phys.Vec3(v.x + (fx * sp * dir - v.x) * grip, v.y, v.z + (fz * sp * dir - v.z) * grip);
        }
        // потолок скорости: на льду трение почти 1, и без него буст разгонял лодку бесконечно — её подбрасывало в небо
        double max = 1.5 + bonus * 3;
        double k = bonus > 0 && sp < max ? 1 + 0.012 * bonus / 0.1 : 1;
        b.setDeltaMovement(v.x * k, Math.min(v.y, 0.5), v.z * k);
    }

    private static java.lang.reflect.Field SPIN;
    private static boolean spinFailed;
    private static Float spin(net.minecraft.world.entity.vehicle.Boat b) {
        if (spinFailed) return null;
        try {
            if (SPIN == null) { SPIN = net.minecraft.world.entity.vehicle.Boat.class.getDeclaredField("deltaRotation"); SPIN.setAccessible(true); }
            return SPIN.getFloat(b);
        } catch (Exception e) { spinFailed = true; return null; }
    }
    private static void spin(net.minecraft.world.entity.vehicle.Boat b, float v) { try { SPIN.setFloat(b, v); } catch (Exception e) { spinFailed = true; } }

    private static boolean calibrated = false;
    private static net.minecraft.client.CameraType savedCam = null;
    private static float camYaw;

    private static boolean liftCam() { return (ClientState.cineKind == 5 || ClientState.cineKind == 8) && System.currentTimeMillis() - ClientState.cineStart < ClientState.cineLen; }

    private static void onTick() {
        Minecraft mc = Minecraft.getInstance();
        boatBoost(mc);
        if (liftCam() && savedCam == null && mc.player != null) {
            savedCam = mc.options.getCameraType();
            camYaw = mc.player.getYRot();
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        } else if (!liftCam() && savedCam != null) {
            mc.options.setCameraType(savedCam);
            savedCam = null;
        }
        if (!calibrated && mc.font != null && mc.player != null) {
            calibrated = true;
            com.mojang.logging.LogUtils.getLogger().info("[UI-FIT] калибровка шрифта: Поко X3 Про={} Золотая морковь={} Жетоны: 200={} ★✦={}",
                    mc.font.width("Поко X3 Про"), mc.font.width("Золотая морковь"), mc.font.width("Жетоны: 200"), mc.font.width("★✦"));
        }
        while (PHONE.consumeClick()) if (mc.player != null && mc.screen == null) PacketDistributor.sendToServer(new Network.PhoneCmd("open"));
        for (int k = 1; k <= 2; k++) while (SKILL_KEYS[k].consumeClick()) {
            if (mc.player == null) continue;
            if (k == 1) { PacketDistributor.sendToServer(new Network.PhoneCmd("skill:2")); continue; }
            int need = k == 1 ? 10 : 35;
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(ClientState.level < need
                    ? "Слот навыка " + (k + 1) + " откроется на " + need + " уровне."
                    : "Слот навыка " + (k + 1) + " пуст — навыки для него появятся с этажом 3.").withStyle(net.minecraft.ChatFormatting.GRAY), true);
        }
        while (DASH.consumeClick()) {
            if (mc.player == null || mc.level == null) continue;
            long now = mc.level.getGameTime();
            if (mc.player.isPassenger() || now < ClientState.dashReadyAt) continue;
            ClientState.dashReadyAt = now + com.pvpciv.game.DashSkill.COOLDOWN_TICKS;
            PacketDistributor.sendToServer(new Network.Dash());
        }
    }
}
