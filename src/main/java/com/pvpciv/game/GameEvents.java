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
        bus.addListener(GameEvents::onMount);
        bus.addListener((net.neoforged.neoforge.event.entity.living.LivingDeathEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.world.entity.Mob m && !m.level().isClientSide) com.pvpciv.floor.EliteSpots.onDeath(m);
        });
        bus.addListener(GameEvents::onNameFormat);
        // реген от сытости и эффекта «Регенерация» — в процентах от макс. HP (ваниль лечит по 1 HP — при 100 HP это вечность)
        bus.addListener((net.neoforged.neoforge.event.entity.living.LivingHealEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && e.getAmount() <= 1.0001f && p.getMaxHealth() > 20)
                e.setAmount(e.getAmount() * p.getMaxHealth() / 20f);
        });
        bus.addListener((net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish e) -> {
            if (e.getEntity() instanceof ServerPlayer p && e.getItem().has(net.minecraft.core.component.DataComponents.FOOD)) Marks.onEat(p);
        });
        bus.addListener((net.neoforged.neoforge.event.entity.living.LivingDeathEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) Marks.onDeath(p);
        });
        CombatEvents.register(bus);
    }

    private static void onReload(AddReloadListenerEvent e) { e.addListener(LootTables.INSTANCE); }

    private static void onStarted(ServerStartedEvent e) {
        MinecraftServer server = e.getServer();
        ServerLevel lvl = com.pvpciv.world.CivWorld.level(server);
        CivData data = CivData.get(server);
        if (data.version >= 10 && data.version < com.pvpciv.world.WorldBuild.VERSION) com.pvpciv.world.WorldBuild.patch(server, data.version);
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
        for (var bd : BossManager.DEFS) data.bossNext.put(bd.id(), 0L);   // при запуске сервера боссы сразу живы (просыпаются, когда кто-то рядом)
        data.setDirty();
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { Story.onLogin(p); Gates.sync(p); }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { Gates.forget(p); Falling.canopy(p, false); Vampire.onLogout(p); DialogueManager.forget(p); Marks.forget(p); Taxi.cancel(p); CombatEvents.forget(p); }
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
        EliteSkills.tick(server);
        if (ticks % 60 == 0) SpawnZones.tick(server);
        if (ticks % 20 == 7) com.pvpciv.floor.EliteSpots.tick(server);
        if (ticks % 20 == 0) for (ServerPlayer p : server.getPlayerList().getPlayers()) Story.tickPlayer(p);
        if (ticks % 10 == 0) for (ServerPlayer p : server.getPlayerList().getPlayers()) com.pvpciv.inv.Bag.sweep(p);
        if (ticks % 5 == 0) for (ServerPlayer p : server.getPlayerList().getPlayers()) Falling.tick(p);
        if (ticks % 10 == 3) for (ServerPlayer p : server.getPlayerList().getPlayers()) { Marks.tick(p); Taxi.tick(p); }
        if (ticks % 20 == 7) for (ServerPlayer p : server.getPlayerList().getPlayers()) { FoodApp.tick(p); Gates.tick(p); }
        if (ticks % 100 == 41) Gates.ensure(server);
        if (ticks % 100 == 0) {
            LunaEntity.ensure(server); TraderEntity.ensure(server);
            com.pvpciv.entity.CarmillaEntity.ensure(server); com.pvpciv.entity.EleonoraEntity.ensure(server);
            com.pvpciv.floor.Floor2.ensureNpcs(server);
            com.pvpciv.world.Monorail.ensure(server);
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
    /** Из кабины лифта на ходу не выйти. */
    private static void onMount(net.neoforged.neoforge.event.entity.EntityMountEvent e) {
        if (e.isDismounting() && e.getEntityBeingMounted() instanceof com.pvpciv.entity.LiftCabin c && c.ride() && c.locked && c.isAlive()) e.setCanceled(true);
        // мобов в лодки не сажаем (абуз с беты: мобы в лодке беспомощны)
        if (e.isMounting() && e.getEntityBeingMounted() instanceof net.minecraft.world.entity.vehicle.Boat
                && e.getEntityMounting() instanceof net.minecraft.world.entity.Mob) e.setCanceled(true);
    }

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
        if (idle > 200 || e.getEntity().getY() < 0) {   // пустая лодка исчезает через 10 с
            if (e.getEntity().level() instanceof ServerLevel sl) sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, e.getEntity().getX(), e.getEntity().getY() + 0.5, e.getEntity().getZ(), 12, 0.5, 0.2, 0.5, 0.02);
            e.getEntity().discard();
        }
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

    /** /beta текст — пожелания и баги в файл pvpciv_beta.txt рядом с сервером/игрой. */
    private static int beta(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String text = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "text").replace('\n', ' ').trim();
        if (text.isEmpty()) return 0;
        java.nio.file.Path f = p.server.getServerDirectory().resolve("pvpciv_beta.txt");
        String line = String.format("[%s] %s | v%s | ур.%d | %s | %d %d %d | %s%n",
                java.time.LocalDateTime.now().withNano(0).toString().replace('T', ' '), p.getGameProfile().getName(),
                net.neoforged.fml.ModList.get().getModFileById(com.pvpciv.PvpCiv.MODID).versionString(),
                Progress.d(p).level, com.pvpciv.floor.Zones.at(p).name(), p.getBlockX(), p.getBlockY(), p.getBlockZ(), text);
        try {
            java.nio.file.Files.writeString(f, line, java.nio.charset.StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException ex) {
            p.sendSystemMessage(Component.literal("Не удалось записать: " + ex.getMessage()).withStyle(ChatFormatting.RED));
            return 0;
        }
        p.sendSystemMessage(Component.literal("✎ Записано в бета-журнал. Спасибо!").withStyle(ChatFormatting.AQUA));
        return 1;
    }

    private static void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("spawn")
                .executes(c -> {
                    c.getSource().sendSuccess(() -> Component.literal("/spawn — застрял? Это УБЬЁТ персонажа (как обычная смерть: штраф и возрождение в квартире). Подтверди: /spawn confirm").withStyle(ChatFormatting.YELLOW), false);
                    return 1;
                })
                .then(Commands.literal("confirm").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (Vampire.jailed(p)) { c.getSource().sendFailure(Component.literal("Из клетки Кармиллы так не сбежать.")); return 0; }
                    p.hurt(p.damageSources().genericKill(), Float.MAX_VALUE);
                    return 1;
                })));
        e.getDispatcher().register(Commands.literal("beta")
                .then(Commands.argument("text", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(GameEvents::beta)));
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
                            boolean f2 = switch (w) { case "f2", "sewer", "club", "throne", "tower", "gym" -> true; default -> false; };
                            if (f2) to = switch (w) {
                                case "sewer" -> com.pvpciv.floor.Floor2.polar(22, com.pvpciv.floor.Floor2.MID, com.pvpciv.floor.Floor2.YS);
                                case "club" -> com.pvpciv.floor.Floor2.polar(315, 100, com.pvpciv.floor.Floor2.Y);
                                case "throne" -> com.pvpciv.floor.Floor2.RAT_THRONE.offset(10, 0, 0);
                                case "tower" -> com.pvpciv.floor.Floor2.TOWER.offset(-8, 0, 0);
                                case "gym" -> com.pvpciv.floor.Floor2.GYM.south(3);
                                default -> net.minecraft.core.BlockPos.containing(com.pvpciv.floor.Floor2.LIFT_ARRIVE);
                            };
                            if (w.equals("roof")) to = net.minecraft.core.BlockPos.containing(com.pvpciv.world.RoofBuilder.ARRIVE);
                            p.teleportTo(f2 ? com.pvpciv.world.CivWorld.f2(p.server) : com.pvpciv.world.CivWorld.level(p.server), to.getX() + 0.5, to.getY(), to.getZ() + 0.5, p.getYRot(), 0);
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
                .then(Commands.literal("rebuild2").requires(s -> s.hasPermission(2)).executes(c -> {
                    com.pvpciv.floor.Floor2.build(com.pvpciv.world.CivWorld.f2(c.getSource().getServer()));
                    com.pvpciv.world.RoofBuilder.build(com.pvpciv.world.CivWorld.level(c.getSource().getServer()));
                    c.getSource().sendSuccess(() -> Component.literal("Этаж 2 и крыша перестроены."), false);
                    return 1;
                }))
                .then(Commands.literal("phone").requires(s -> s.hasPermission(2)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    StoryData d = Story.data(p);
                    d.eleStage = 2; d.liftOk = true; d.trophies |= 15; d.eleTokens += 200;
                    p.setData(ModRegistry.STORY.get(), d);
                    com.pvpciv.inv.Bag.give(p, new ItemStack(ModRegistry.PHONE.get()));
                    c.getSource().sendSuccess(() -> Component.literal("Тест: телефон, этаж 2, все трофеи, +200 жетонов."), false);
                    return 1;
                }))
                .then(Commands.literal("call").requires(s -> s.hasPermission(2)).executes(c -> {
                    Marks.testCall(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("unlock2").requires(s -> s.hasPermission(2)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    StoryData d = Story.data(p);
                    d.trophies |= 3; d.showcase |= 3;
                    p.setData(ModRegistry.STORY.get(), d);
                    c.getSource().sendSuccess(() -> Component.literal("Трофеи этажа 1 засчитаны и выставлены (тест). Поговори с Луной у лифта."), false);
                    return 1;
                }))
                .then(Commands.literal("jail").requires(s -> s.hasPermission(2)).executes(c -> {
                    Vampire.abduct(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("boss").requires(s -> s.hasPermission(2)).executes(c -> {
                    BossManager.forceSpawn(c.getSource().getServer(), "brute");
                    c.getSource().sendSuccess(() -> Component.literal("Громила проснётся, как только рядом с логовом будет игрок."), false);
                    return 1;
                }).then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(BossManager.DEFS.stream().map(BossManager.Def::id), b))
                        .executes(c -> {
                            String id = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "id");
                            if (!BossManager.forceSpawn(c.getSource().getServer(), id)) { c.getSource().sendFailure(Component.literal("Нет такого босса или он уже жив.")); return 0; }
                            c.getSource().sendSuccess(() -> Component.literal("Босс «" + id + "» проснётся, как только рядом будет игрок."), false);
                            return 1;
                        })))
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
