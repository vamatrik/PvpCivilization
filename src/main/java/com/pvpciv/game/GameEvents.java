package com.pvpciv.game;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.pvpciv.combat.CombatEvents;
import com.pvpciv.data.CivData;
import com.pvpciv.data.StoryData;
import com.pvpciv.entity.GuardEntity;
import com.pvpciv.entity.LunaEntity;
import com.pvpciv.entity.TraderEntity;
import com.pvpciv.floor.BossManager;
import com.pvpciv.floor.Floor1;
import com.pvpciv.floor.SpawnZones;
import com.pvpciv.item.BoatSummonItem;
import com.pvpciv.loot.LootTables;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.DormBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

public final class GameEvents {
    private record Task(long at, Runnable r) {}
    private static final List<Task> TASKS = new ArrayList<>();
    private static long ticks = 0;

    public static void later(MinecraftServer server, int delay, Runnable r) { TASKS.add(new Task(ticks + delay, r)); }

    public static void register(IEventBus bus) {
        bus.addListener(GameEvents::onStarted);
        bus.addListener(GameEvents::onLogin);
        bus.addListener(GameEvents::onLogout);
        bus.addListener(GameEvents::onRespawn);
        bus.addListener(GameEvents::onTick);
        bus.addListener(GameEvents::onBreak);
        bus.addListener(GameEvents::onPlace);
        bus.addListener(GameEvents::onToss);
        bus.addListener(GameEvents::onCommands);
        bus.addListener(GameEvents::onReload);
        bus.addListener(GameEvents::onAttack);
        bus.addListener(GameEvents::onBoatTick);
        bus.addListener(BossManager::onDeath);
        bus.addListener(GameEvents::onPickup);
        bus.addListener(GameEvents::onEntityJoin);
        bus.addListener(GameEvents::onRightClickBlock);
        bus.addListener(GameEvents::onNameFormat);
        CombatEvents.register(bus);
    }

    private static void onReload(AddReloadListenerEvent e) { e.addListener(LootTables.INSTANCE); }

    private static void onStarted(ServerStartedEvent e) {
        MinecraftServer server = e.getServer();
        ServerLevel lvl = com.pvpciv.world.CivWorld.level(server);
        CivData data = CivData.get(server);
        if (data.version < com.pvpciv.world.WorldBuild.VERSION) {
            com.mojang.logging.LogUtils.getLogger().info("[PvpCiv] Building world 1 in dimension pvpciv:world1 (v{} -> v{})", data.version, com.pvpciv.world.WorldBuild.VERSION);
            com.pvpciv.world.WorldBuild.buildAll(server, false);
        }
        GameRules r = lvl.getGameRules();
        r.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        r.getRule(GameRules.RULE_DOINSOMNIA).set(false, server);
        r.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, server);
        r.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, server);
        r.getRule(GameRules.RULE_DO_WARDEN_SPAWNING).set(false, server);
        r.getRule(GameRules.RULE_KEEPINVENTORY).set(true, server);
        r.getRule(GameRules.RULE_SPAWN_RADIUS).set(0, server);
        r.getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, server);
        r.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        r.getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(false, server);
        r.getRule(GameRules.RULE_LIMITED_CRAFTING).set(true, server);
        lvl.setWeatherParameters(6000000, 0, false, false);
        server.overworld().setWeatherParameters(6000000, 0, false, false);
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Story.onLogin(p);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) DialogueManager.forget(p);
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!e.isEndConquered() && e.getEntity() instanceof ServerPlayer p) Story.onRespawn(p);
    }

    private static void onTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        ticks++;
        if (!TASKS.isEmpty()) {
            List<Task> due = new ArrayList<>();
            TASKS.removeIf(t -> { if (t.at <= ticks) { due.add(t); return true; } return false; });
            due.forEach(t -> t.r.run());
        }
        ArenaManager.tick(server);
        BossManager.tick(server);
        if (ticks % 60 == 0) SpawnZones.tick(server);
        if (ticks % 20 == 0) for (ServerPlayer p : server.getPlayerList().getPlayers()) Story.tickPlayer(p);
        if (ticks % 10 == 0) for (ServerPlayer p : server.getPlayerList().getPlayers()) com.pvpciv.inv.Bag.sweep(p);
        if (ticks % 100 == 0) {
            LunaEntity.ensure(server); TraderEntity.ensure(server);
            com.pvpciv.entity.CarmillaEntity.ensure(server); com.pvpciv.entity.EleonoraEntity.ensure(server);
        }
        com.pvpciv.world.IceTrack.tick(server);
    }

    /** Подобранное сразу уходит в сумку. */
    private static void onPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || p.isCreative()) return;
        var ie = e.getItemEntity();
        if (ie.hasPickUpDelay()) return;
        ItemStack st = ie.getItem();
        if (st.getItem() instanceof com.pvpciv.item.SparkItem) return;
        int before = st.getCount();
        ItemStack rest = com.pvpciv.inv.Bag.insert(p, st.copy());
        if (rest.getCount() == before) return;
        p.take(ie, before - rest.getCount());
        if (rest.isEmpty()) {
            ie.discard();
            e.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
        } else ie.setItem(rest);
        p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.2f, 1.4f + p.getRandom().nextFloat() * 0.4f);
    }

    /** Никаких ванильных мобов: в мире живут только мобы цивилизации. */
    private static void onEntityJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide || e.loadedFromDisk()) return;
        if (e.getEntity() instanceof net.minecraft.world.entity.Mob m && !(m instanceof com.pvpciv.entity.StoryNpc)
                && com.pvpciv.combat.Mobs.level(m) <= 0) {
            if (com.pvpciv.registry.ModMobs.isCiv(m)) com.pvpciv.combat.Mobs.setupDefault(m);
            else e.setCanceled(true);
        }
    }

    /** Комната Луны: дневник и шкатулка. */
    private static void onRightClickBlock(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        if (!com.pvpciv.world.CivWorld.is(p.level())) return;
        net.minecraft.core.BlockPos pos = e.getPos();
        if (pos.equals(DormBuilder.LUNA_DIARY)) {
            e.setCanceled(true);
            if (!DialogueManager.inDialogue(p)) DialogueManager.open(p, null, Dialogues.lunaDiary(p));
        } else if (pos.equals(DormBuilder.LUNA_BOX)) {
            e.setCanceled(true);
            Dialogues.lunaBox(p);
        }
    }

    /** Активный титул — над ником и в чате. */
    private static void onNameFormat(PlayerEvent.NameFormat e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Component pre = com.pvpciv.progress.Titles.prefix(Progress.d(p));
        if (pre != null) e.setDisplayname(pre.copy().append(e.getDisplayname()));
    }

    private static boolean ownedBoat(net.minecraft.world.entity.Entity e) {
        return e instanceof Boat b && b.getPersistentData().hasUUID(BoatSummonItem.OWNER_TAG);
    }

    /** Чужую лодку не сломать; свою — удар убирает её обратно в предмет. */
    private static void onAttack(AttackEntityEvent e) {
        if (!ownedBoat(e.getTarget())) return;
        e.setCanceled(true);
        if (e.getEntity() instanceof ServerPlayer p && p.getUUID().equals(e.getTarget().getPersistentData().getUUID(BoatSummonItem.OWNER_TAG))) {
            e.getTarget().ejectPassengers();
            e.getTarget().discard();
            p.displayClientMessage(Component.literal("Лодка убрана").withStyle(ChatFormatting.AQUA), true);
        }
    }

    /** Брошенные лодки исчезают через 5 минут без пассажиров. */
    private static void onBoatTick(EntityTickEvent.Post e) {
        if (e.getEntity().level().isClientSide || !ownedBoat(e.getEntity())) return;
        CompoundTag t = e.getEntity().getPersistentData();
        if (e.getEntity().isVehicle()) {
            t.putInt("idle", 0);
            return;
        }
        int idle = t.getInt("idle") + 1;
        t.putInt("idle", idle);
        if (idle > 20 * 60 * 5 || e.getEntity().getY() < 0) e.getEntity().discard();
    }

    private static void onBreak(BlockEvent.BreakEvent e) {
        if (!e.getPlayer().isCreative()) e.setCanceled(true);
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof Player p && !p.isCreative()) e.setCanceled(true);
    }

    private static void onToss(ItemTossEvent e) {
        ItemStack st = e.getEntity().getItem();
        if (st.is(ModRegistry.KEY_CARD.get()) || st.getItem() instanceof BoatSummonItem) {
            e.setCanceled(true);
            e.getPlayer().getInventory().add(st.copy());
            e.getPlayer().displayClientMessage(Component.literal("Это выбрасывать нельзя. Вспомни Гришу.").withStyle(ChatFormatting.RED), true);
        }
    }

    private static void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("civ")
                .then(Commands.literal("home").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (Vampire.jailed(p)) { p.displayClientMessage(Component.literal("Ты в клетке у Кармиллы. Жди.").withStyle(ChatFormatting.RED), true); return 0; }
                    CivData.Apartment a = CivData.get(p.server).byId(Story.data(p).apartment);
                    if (a != null) Story.toApartment(p, a);
                    return 1;
                }))
                .then(Commands.literal("reset").requires(s -> s.hasPermission(2)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    StoryData d = Story.data(p);
                    d.stage = Story.NEW; d.level = 1; d.xp = 0; d.money = 0;
                    p.setData(ModRegistry.STORY.get(), d);
                    p.getInventory().clearContent();
                    CivData.Apartment a = CivData.get(p.server).byId(d.apartment);
                    if (a != null) { Story.toApartment(p, a); GuardEntity.ensureFor(p, a); }
                    Story.ensureKit(p);
                    Progress.applyStats(p);
                    Story.sync(p);
                    c.getSource().sendSuccess(() -> Component.literal("Прогресс сброшен."), false);
                    return 1;
                }))
                .then(Commands.literal("stage").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("n", IntegerArgumentType.integer(0, 1)).executes(c -> {
                            Story.setStage(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "n"));
                            return 1;
                        })))
                .then(Commands.literal("level").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 210)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            StoryData d = Story.data(p);
                            d.level = IntegerArgumentType.getInteger(c, "n"); d.xp = 0;
                            p.setData(ModRegistry.STORY.get(), d);
                            Progress.applyStats(p);
                            Story.sync(p);
                            return 1;
                        })))
                .then(Commands.literal("money").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("n", IntegerArgumentType.integer(0)).executes(c -> {
                            Progress.addMoney(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "n"), true);
                            return 1;
                        })))
                .then(Commands.literal("go").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("place", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            String w = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "place");
                            net.minecraft.core.BlockPos to = switch (w) {
                                case "south" -> Floor1.CAMP; case "north" -> com.pvpciv.floor.FloorNorth.OUTPOST; case "basement" -> com.pvpciv.world.Basement.CARMILLA_SPOT.south(3);
                                case "track" -> DormBuilder.NORTH_DOCK; case "luna" -> DormBuilder.LUNA_SPOT.south(2); case "crypt" -> com.pvpciv.floor.FloorNorth.CRYPT.south(16);
                                case "lair" -> Floor1.LAIR.north(14); case "ruins" -> Floor1.RUINS; case "eleonora" -> DormBuilder.ELEONORA_SPOT.north(1);
                                default -> DormBuilder.ATRIUM_CENTER;
                            };
                            p.teleportTo(com.pvpciv.world.CivWorld.level(p.server), to.getX() + 0.5, to.getY(), to.getZ() + 0.5, p.getYRot(), 0);
                            return 1;
                        })))
                .then(Commands.literal("cd").requires(s -> s.hasPermission(2)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    StoryData d = Story.data(p);
                    d.questCooldown = 0; d.jailUntil = 0;
                    p.setData(ModRegistry.STORY.get(), d);
                    com.pvpciv.world.Basement.setDoor(com.pvpciv.world.CivWorld.level(p.server), true);
                    Story.sync(p);
                    c.getSource().sendSuccess(() -> Component.literal("Кулдаун контрактов и заточение сброшены."), false);
                    return 1;
                }))
                .then(Commands.literal("jail").requires(s -> s.hasPermission(2)).executes(c -> {
                    Vampire.abduct(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("boss").requires(s -> s.hasPermission(2)).executes(c -> {
                    BossManager.forceSpawn(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("Громила проснётся, как только рядом с логовом будет игрок."), false);
                    return 1;
                }))
                .then(Commands.literal("loot").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("table", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            for (ItemStack st : LootTables.roll(com.mojang.brigadier.arguments.StringArgumentType.getString(c, "table"), p.getRandom(), 1.0))
                                if (!p.getInventory().add(st)) p.drop(st, false);
                            return 1;
                        })))
                .then(Commands.literal("rebuild").requires(s -> s.hasPermission(2)).executes(c -> {
                    com.pvpciv.world.WorldBuild.buildAll(c.getSource().getServer(), true);
                    c.getSource().sendSuccess(() -> Component.literal("Мир 1 перестроен: общежитие, подвал, Пепельная пустошь, Инеевый Кряж, трассы."), true);
                    return 1;
                })));
    }
}
