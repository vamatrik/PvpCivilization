package com.pvpciv.game;

import com.pvpciv.data.CivData;
import com.pvpciv.data.StoryData;
import com.pvpciv.entity.GuardEntity;
import com.pvpciv.floor.BossManager;
import com.pvpciv.floor.Zones;
import com.pvpciv.item.BoatSummonItem;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.item.KeyCardItem;
import com.pvpciv.loot.Gear;
import com.pvpciv.loot.Rarity;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.DormBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Онбординг, комплект новичка, синхронизация HUD. */
public final class Story {
    public static final int NEW = 0, GUARD_DONE = 1;
    private static final Map<UUID, Zones.Zone> LAST_ZONE = new HashMap<>();

    public static StoryData data(ServerPlayer p) { return p.getData(ModRegistry.STORY.get()); }

    public static String objective(int stage) {
        if (stage == NEW) return "Кто-то стучит в дверь. Открой её ключ-картой (ПКМ картой по двери) и выйди к гостю.";
        return "Этаж 1 на юге по Ледяному Тракту: мобы, тайники в руинах, данж-арена и босс Громила. Звёзды точи в кузнице своей квартиры. Хлам продавай Сороке в атриуме.";
    }

    public static void sync(ServerPlayer p) {
        StoryData d = data(p);
        Zones.Zone z = Zones.at(p.position());
        int boss = BossManager.secondsLeft(p.server);
        PacketDistributor.sendToPlayer(p, new Network.SyncState(d.stage, objective(d.stage), d.level, d.xp, Progress.xpToNext(d.level),
                d.money, z.name(), z.type().ordinal(), boss));
    }

    /** Раз в секунду: зона, таймер босса. */
    public static void tickPlayer(ServerPlayer p) {
        Zones.Zone z = Zones.at(p.position());
        Zones.Zone last = LAST_ZONE.put(p.getUUID(), z);
        if (last != null && last != z && z != Zones.ABYSS) {
            String sub = switch (z.type()) { case SAFE -> "Безопасная зона"; case PVE -> "PvE-зона"; case PVP -> "⚔ PvP-зона: игроки могут атаковать!"; };
            p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 15));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub).withStyle(z.type() == com.pvpciv.floor.Zones.Type.PVP ? ChatFormatting.RED : ChatFormatting.GRAY)));
            String shortName = z.name().contains("· ") ? z.name().substring(z.name().lastIndexOf("· ") + 2) : z.name();
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(shortName).withStyle(s -> s.withColor(0xFFD9A0))));
        }
        sync(p);
    }

    public static void setStage(ServerPlayer p, int stage) {
        StoryData d = data(p);
        d.stage = stage;
        p.setData(ModRegistry.STORY.get(), d);
        sync(p);
    }

    public static void title(ServerPlayer p, String title, String sub, int color) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub).withStyle(ChatFormatting.GRAY)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title).withStyle(s -> s.withColor(color).withBold(true))));
    }

    public static void say(ServerPlayer p, String who, int color, String msg) {
        p.sendSystemMessage(Component.literal("[" + who + "] ").withStyle(s -> s.withColor(color).withBold(true))
                .append(Component.literal(msg).withStyle(s -> s.withColor(0xFFFFFF).withBold(false))));
    }

    /** Объявление на весь сервер о редком дропе. */
    public static void announceRareDrop(Level level, ItemStack st) {
        if (!(level instanceof ServerLevel sl)) return;
        boolean rare = st.getItem() instanceof CivWeaponItem && Gear.rarity(st).ordinal() >= Rarity.EPIC.ordinal();
        boolean blessed = st.is(ModRegistry.BLESSED_STAR_W1.get());
        if (!rare && !blessed) return;
        Component msg = Component.literal("✦ Выпал редкий предмет: ").withStyle(ChatFormatting.GOLD).append(st.getHoverName());
        for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) p.sendSystemMessage(msg);
    }

    // ------------------------------------------------------------------ kit
    private static boolean has(Inventory inv, java.util.function.Predicate<ItemStack> pred) {
        return com.pvpciv.inv.Bag.has(inv.player, pred);
    }

    public static void ensureKit(ServerPlayer p) {
        Inventory inv = p.getInventory();
        CivData civ = CivData.get(p.server);
        StoryData d = data(p);
        CivData.Apartment apt = civ.byId(d.apartment);
        if (apt != null && !has(inv, s -> s.is(ModRegistry.KEY_CARD.get()) && p.getUUID().equals(KeyCardItem.owner(s))))
            inv.add(KeyCardItem.create(p, apt.number));
        if (!has(inv, s -> s.getItem() instanceof CivWeaponItem) && !Forge.busy(p))
            inv.add(new ItemStack(ModRegistry.NOVICE_SWORD.get()));
        if (!has(inv, s -> s.getItem() instanceof BoatSummonItem))
            inv.add(new ItemStack(ModRegistry.OAK_BOAT_T1.get()));
        if (!d.starterFood) {
            d.starterFood = true;
            p.setData(ModRegistry.STORY.get(), d);
            inv.add(new ItemStack(ModRegistry.DORM_STEW.get(), 4));
        }
        // старые вещи из эпизода 1
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.is(Items.OAK_BOAT) || st.is(Items.WOODEN_SWORD)) inv.setItem(i, ItemStack.EMPTY);
            if (st.is(ModRegistry.ASH_BLADE.get())) inv.setItem(i, Gear.withRarity(new ItemStack(ModRegistry.RUSTY_CLEAVER.get()), Rarity.UNCOMMON));
        }
    }

    // ------------------------------------------------------------------ onboarding
    public static void onLogin(ServerPlayer p) {
        CivData civ = CivData.get(p.server);
        StoryData d = data(p);
        d.dash = true;
        if (d.stage > GUARD_DONE) d.stage = GUARD_DONE;
        p.setData(ModRegistry.STORY.get(), d);
        CivData.Apartment apt = civ.byId(d.apartment);
        if (apt == null || !p.getUUID().equals(apt.owner)) {
            apt = civ.byOwner(p.getUUID());
            if (apt == null) apt = civ.findFree();
            if (apt == null) {
                p.sendSystemMessage(Component.literal("Общежитие переполнено! Спишь в атриуме на лавочке.").withStyle(ChatFormatting.RED));
                return;
            }
            apt.owner = p.getUUID();
            apt.ownerName = p.getGameProfile().getName();
            civ.setDirty();
            d.apartment = apt.id;
            p.setData(ModRegistry.STORY.get(), d);
            DormBuilder.updateApartmentSign(p.serverLevel(), apt);
            toApartment(p, apt);
            title(p, "PvP ЦИВИЛИЗАЦИЯ", "Нижний Шпиль • Этаж 1: Пепельная пустошь", 0x7FDBFF);
            p.sendSystemMessage(Component.literal("Ты открываешь глаза. Незнакомый потолок. Квартира №" + apt.number + ".")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        p.setRespawnPosition(Level.OVERWORLD, apt.spawn, apt.yaw, true, false);
        ensureKit(p);
        ItemStack pending = Forge.PENDING.remove(p.getUUID());
        if (pending != null && !p.getInventory().add(pending)) p.drop(pending, false);
        Progress.applyStats(p);
        if (d.stage == NEW) GuardEntity.ensureFor(p, apt);
        sync(p);
    }

    public static void toApartment(ServerPlayer p, CivData.Apartment apt) {
        ServerLevel lvl = p.server.overworld();
        BlockPos s = apt.spawn;
        p.resetFallDistance();
        p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        p.teleportTo(lvl, s.getX() + 0.5, s.getY(), s.getZ() + 0.5, apt.yaw, 0);
        p.resetFallDistance();
    }

    public static void onRespawn(ServerPlayer p) {
        StoryData d = data(p);
        CivData.Apartment apt = CivData.get(p.server).byId(d.apartment);
        if (apt == null) return;
        toApartment(p, apt);
        d.deaths++;
        p.setData(ModRegistry.STORY.get(), d);
        ensureKit(p);
        Progress.applyStats(p);
        p.setHealth(p.getMaxHealth());
        String[] lines = {
                "Ты просыпаешься в своей кровати. Голова раскалывается... Кажется, ты что-то забыл.",
                "Снова потолок квартиры. Ты пытаешься вспомнить, что ел вчера на завтрак. Не получается.",
                "Ты очнулся дома. В памяти — дыра размером с монетку. Ерунда, наверное.",
        };
        p.sendSystemMessage(Component.literal(lines[d.deaths % lines.length]).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        sync(p);
    }

    public static ItemStack noteFromZero() {
        ItemStack s = new ItemStack(Items.PAPER);
        s.set(DataComponents.CUSTOM_NAME, Component.literal("Мятая записка").withStyle(st -> st.withItalic(false).withColor(0xFF5555)));
        s.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("«Не умирай слишком часто.").withStyle(ChatFormatting.GRAY),
                Component.literal(" Они забирают не только Искры.»").withStyle(ChatFormatting.GRAY),
                Component.literal("                      — 0").withStyle(ChatFormatting.DARK_RED))));
        s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return s;
    }
}
