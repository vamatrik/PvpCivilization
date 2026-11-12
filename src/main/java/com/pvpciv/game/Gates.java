package com.pvpciv.game;

import com.pvpciv.data.CivData;
import com.pvpciv.data.StoryData;
import com.pvpciv.entity.GateEntity;
import com.pvpciv.floor.Floor1;
import com.pvpciv.floor.Floor2;
import com.pvpciv.floor.FloorNorth;
import com.pvpciv.inv.Bag;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.CivWorld;
import com.pvpciv.world.DormBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.function.Supplier;

/**
 * Врата телепортации (план 0.9, сделано в 0.8.6).
 * • Врата по миру: подошёл — нашёл; сломанные чинятся ресурсами своей локации (иногда — редкой вещью). Починил — перенос бесплатный.
 * • Домашние врата в квартире: всегда работают, ведут на любые починенные врата открытых этажей; уровень = сколько этажей доступно,
 *   улучшение — ресурсы нового этажа и Искры. С врат по миру — только по своему этажу и домой.
 * • Перенос: 3 с стоять спокойно, нельзя в бою, из темницы и во время звонка; перезарядка 30 с.
 */
public final class Gates {
    public record Cost(Supplier<Item> item, int n) {}
    public record Gate(String id, String name, String desc, int floor, String style, Supplier<BlockPos> pos, int money, Cost... costs) {}

    public static final List<Gate> ALL = List.of(
            new Gate("g_roof", "Крыша общежития", "Зал Небесного лифта на крыше. Отсюда — на этаж 2 и выше.", 1, "ice",
                    () -> new BlockPos(8, com.pvpciv.world.RoofBuilder.RY, -27), 20, new Cost(() -> ModRegistry.RUSTY_GEAR.get(), 1)),
            new Gate("g_tract", "Пристань Тракта", "Начало Ледяного Тракта: отсюда лодки уходят на юг, к Пустоши.", 1, "ash",
                    () -> DormBuilder.DOCK_CENTER.offset(5, 0, 3), 20, new Cost(() -> ModRegistry.RUSTY_GEAR.get(), 2)),
            new Gate("g_camp", "Лагерь Пустоши", "Последний костёр перед пеплом. Здесь собираются те, кто идёт к Громиле.", 1, "ash",
                    () -> Floor1.CAMP.offset(8, 0, -4), 40, new Cost(() -> ModRegistry.ASH_DUST.get(), 4), new Cost(() -> ModRegistry.CHARRED_COIN.get(), 2)),
            new Gate("g_ruins", "Руины", "Обгоревшие стены старого поселения. Пепельные капралы любят эти коридоры.", 1, "ash",
                    () -> Floor1.RUINS.offset(10, 0, 12), 60, new Cost(() -> ModRegistry.ASH_RELIC.get(), 2), new Cost(() -> ModRegistry.ASH_DUST.get(), 4)),
            new Gate("g_arena", "Арена Пустоши", "Песок, кровь и трибуны без зрителей. Волны мобов — по расписанию.", 1, "ash",
                    () -> Floor1.ARENA.offset(-24, 0, 6), 60, new Cost(() -> ModRegistry.CHARRED_COIN.get(), 6), new Cost(() -> ModRegistry.RUSTY_GEAR.get(), 3)),
            new Gate("g_lair", "У логова Громилы", "Край Пустоши. Дальше — только Гнилой Громила и его аппетит.", 1, "ash",
                    () -> Floor1.LAIR.offset(0, 0, -Floor1.LAIR_R - 9), 80, new Cost(() -> ModRegistry.BRUTE_CRYSTAL.get(), 1), new Cost(() -> ModRegistry.ASH_DUST.get(), 6)),
            new Gate("g_north", "Северная пристань", "Старт Ледяной Трассы. Узко, быстро и обрыв по краям.", 1, "ice",
                    () -> DormBuilder.NORTH_DOCK.offset(5, 0, -3), 20, new Cost(() -> ModRegistry.RUSTY_GEAR.get(), 2)),
            new Gate("g_outpost", "Аванпост Кряжа", "Застава на входе в Инеевый Кряж. Дальше холодно и уровни выше.", 1, "ice",
                    () -> FloorNorth.OUTPOST.offset(7, 0, -6), 40, new Cost(() -> ModRegistry.RIME_SHARD.get(), 4)),
            new Gate("g_lake", "Ледяное озеро", "Замёрзшее озеро с шипами льда. Лёд скользкий — стрелки нет.", 1, "ice",
                    () -> FloorNorth.LAKE.offset(24, 0, 2), 60, new Cost(() -> ModRegistry.RIME_SHARD.get(), 6), new Cost(() -> ModRegistry.FROZEN_LOCKET.get(), 1)),
            new Gate("g_crypt", "У Ледяного склепа", "Вход в склеп Хранительницы. Внутри тихо. Слишком тихо.", 1, "ice",
                    () -> FloorNorth.CRYPT.offset(0, 0, FloorNorth.CRYPT_R + 8), 80, new Cost(() -> ModRegistry.KEEPER_CRYSTAL.get(), 1), new Cost(() -> ModRegistry.RIME_SHARD.get(), 6)),
            new Gate("g_station", "Станция лифта", "Хаб Неонового города: лифт, спортзал Тимофея, парашютист рядом.", 2, "neon",
                    () -> BlockPos.containing(Floor2.LIFT_ARRIVE).offset(7, 0, 4), 30, new Cost(() -> ModRegistry.NEON_TUBE.get(), 2)),
            new Gate("g_neon", "Площадь Неона", "Вывески, копы и шум. Сердце восточных кварталов.", 2, "neon",
                    () -> Floor2.NEON.offset(6, 0, -6), 60, new Cost(() -> ModRegistry.NEON_TUBE.get(), 3), new Cost(() -> ModRegistry.GOLD_CHAIN.get(), 1)),
            new Gate("g_market", "Рынок", "Барахолка под открытым небом. Барыги и гопники делят прилавки.", 2, "neon",
                    () -> Floor2.MARKET.offset(6, 0, 6), 60, new Cost(() -> ModRegistry.GOLD_CHAIN.get(), 3)),
            new Gate("g_park", "Парк", "Единственная зелень этажа. Бездомные дворняги считают его своим.", 2, "neon",
                    () -> Floor2.PARK.offset(-6, 0, 6), 60, new Cost(() -> ModRegistry.NEON_TUBE.get(), 3)),
            new Gate("g_tower", "Небоскрёб «Альтаир»", "Самая высокая башня города. Наверху — парашютист.", 2, "neon",
                    () -> Floor2.polar(15, 76, Floor2.Y), 70, new Cost(() -> ModRegistry.NEON_TUBE.get(), 4), new Cost(() -> ModRegistry.GOLD_CHAIN.get(), 1)),
            new Gate("g_sewer", "Люк у Гоши", "Каморка сантехника в канализации. Крысы, трубы и слухи.", 2, "neon",
                    () -> Floor2.PLUMBER.offset(0, 0, 3), 80, new Cost(() -> ModRegistry.RUBBER_SCRAP.get(), 4)),
            new Gate("g_club", "У клуба «Серра»", "Двери клуба Дона Витторио. Внутрь — только с оружием.", 2, "neon",
                    () -> Floor2.polar(315, 120 - Floor2.CLUB_R - 7, Floor2.Y), 100, new Cost(() -> ModRegistry.DON_RING.get(), 1), new Cost(() -> ModRegistry.GOLD_CHAIN.get(), 4))
    );

    public static Gate byId(String id) { for (Gate g : ALL) if (g.id.equals(id)) return g; return null; }

    /** Стиль кристалла для клиента: ice / ash / neon; домашние — ice. */
    public static String style(String id) { Gate g = byId(id); return g == null ? "ice" : g.style; }

    // ------------------------------------------------------------------ данные игрока
    public static boolean found(StoryData d, String id) { return d.gates.getInt(id) >= 1; }
    public static boolean fixed(StoryData d, String id) { return d.gates.getInt(id) >= 2; }

    /** Уровень домашних врат: 1 — только этаж 1; +1 этаж за улучшение. */
    public static int home(StoryData d) { return Math.max(1, d.homeGate); }

    public static final int[] HOME_UP_MONEY = {0, 0, 800};
    public static final Cost[][] HOME_UP = {{}, {}, {new Cost(() -> ModRegistry.NEON_TUBE.get(), 6), new Cost(() -> ModRegistry.GOLD_CHAIN.get(), 3), new Cost(() -> ModRegistry.RUBBER_SCRAP.get(), 3)}};

    // ------------------------------------------------------------------ мир
    private static ServerLevel lvl(MinecraftServer s, int floor) { return floor == 2 ? CivWorld.f2(s) : CivWorld.level(s); }

    /** Место под врата: площадка 3×3 с твёрдым полом и 3 блоками воздуха над ней, ближайшая к задуманной точке. */
    private static Vec3 spot(MinecraftServer s, Gate g) {
        ServerLevel l = lvl(s, g.floor);
        BlockPos c = g.pos.get();
        for (int r = 0; r <= 12; r++)
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                for (int dy : new int[]{0, 1, -1, 2, -2}) {
                    BlockPos b = c.offset(dx, dy, dz);
                    if (pad(l, b)) return new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
                }
            }
        return Taxi.safe(l, c);
    }

    private static boolean pad(ServerLevel l, BlockPos b) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            BlockPos f = b.offset(x, 0, z);
            if (!l.getBlockState(f.below()).isFaceSturdy(l, f.below(), net.minecraft.core.Direction.UP) || !l.getFluidState(f.below()).isEmpty()) return false;
            for (int y = 0; y < 3; y++) if (!l.getBlockState(f.above(y)).getCollisionShape(l, f.above(y)).isEmpty() || !l.getFluidState(f.above(y)).isEmpty()) return false;
        }
        return true;
    }

    private static final Map<String, Vec3> SPOTS = new HashMap<>();

    /** Раз в 5 с: врата стоят там, где чанк загружен; домашние — в квартирах. */
    public static void ensure(MinecraftServer server) {
        for (Gate g : ALL) {
            ServerLevel l = lvl(server, g.floor);
            if (l == null) continue;
            BlockPos p = g.pos.get();
            if (!l.areEntitiesLoaded(ChunkPos.asLong(p))) continue;
            Vec3 at = SPOTS.computeIfAbsent(g.id, k -> spot(server, g));
            place(l, g.id, at);
        }
        ServerLevel w1 = CivWorld.level(server);
        if (w1 == null || !w1.areEntitiesLoaded(ChunkPos.asLong(HUB_C))) return;
        place(w1, "hub", hubSpot(server));
        if (!oldCleared) {   // старые врата в квартирах (до 0.8.7) убираем
            oldCleared = true;
            for (CivData.Apartment a : CivData.get(server).apartments) {
                Vec3 h = homeSpot(a);
                w1.getEntitiesOfClass(GateEntity.class, new AABB(h, h).inflate(4), e -> e.gateId().startsWith("home:")).forEach(GateEntity::discard);
            }
        }
    }

    /** Большие общие врата в центре атриума общежития: прогресс у каждого свой. */
    private static final BlockPos HUB_C = new BlockPos(0, DormBuilder.Y0, -8);
    private static Vec3 hub;
    private static boolean oldCleared;
    public static Vec3 hubSpot(MinecraftServer s) {
        if (hub != null) return hub;
        ServerLevel l = CivWorld.level(s);
        for (int r = 0; r <= 6 && hub == null; r++)
            for (int dz = -r; dz <= r && hub == null; dz++) for (int dx = -r; dx <= r && hub == null; dx++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                BlockPos b = HUB_C.offset(dx, 0, dz);
                if (pad(l, b)) hub = new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
            }
        if (hub == null) hub = new Vec3(0.5, DormBuilder.Y0, -7.5);
        return hub;
    }

    public static Vec3 homeSpot(CivData.Apartment a) {
        int ix = Integer.signum(a.sign.getX() - a.door.getX()), iz = Integer.signum(a.sign.getZ() - a.door.getZ());
        BlockPos b = a.door.offset(-ix * 2, 0, -iz * 2);
        return new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
    }

    private static void place(ServerLevel l, String id, Vec3 at) {
        List<GateEntity> list = l.getEntitiesOfClass(GateEntity.class, new AABB(at, at).inflate(3), e -> id.equals(e.gateId()));
        for (int i = 1; i < list.size(); i++) list.get(i).discard();
        if (!list.isEmpty()) return;
        GateEntity e = ModRegistry.GATE.get().create(l);
        if (e == null) return;
        e.gateId(id);
        e.moveTo(at.x, at.y, at.z, 0, 0);
        l.addFreshEntity(e);
    }

    /** Раз в секунду: находка врат рядом и перенос, который «заряжается». */
    public static void tick(ServerPlayer p) {
        StoryData d = Progress.d(p);
        int fl = CivWorld.isF2(p.level()) ? 2 : p.level().dimension() == CivWorld.WORLD1 ? 1 : 0;
        for (Gate g : ALL) {
            if (g.floor != fl || found(d, g.id)) continue;
            Vec3 at = SPOTS.get(g.id);
            if (at != null && p.position().distanceToSqr(at) < 64) {
                d.gates.putInt(g.id, 1);
                p.setData(ModRegistry.STORY.get(), d);
                Story.title(p, "Найдены врата", g.name + " — сломаны", 0x8FE3FF);
                p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.PLAYERS, 1f, 0.8f);
            }
        }
        Channel c = CHANNEL.get(p.getUUID());
        if (c == null) return;
        if (p.position().distanceToSqr(c.from) > 1.0 || p.getLastHurtByMobTimestamp() > p.tickCount - 60 || Vampire.jailed(p)) {
            CHANNEL.remove(p.getUUID());
            p.displayClientMessage(Component.literal("Перенос сорван: нужно стоять спокойно и вне боя.").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (++c.sec < 3) {
            p.serverLevel().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 20, 0.5, 0.9, 0.5, 0.02);
            p.displayClientMessage(Component.literal("Перенос… " + (3 - c.sec)).withStyle(ChatFormatting.AQUA), true);
            return;
        }
        CHANNEL.remove(p.getUUID());
        go(p, c.to);
    }

    private static final class Channel { final Vec3 from; final String to; int sec; Channel(Vec3 f, String t) { from = f; to = t; } }
    private static final Map<UUID, Channel> CHANNEL = new HashMap<>();
    private static final Map<UUID, Long> CD = new HashMap<>();

    // ------------------------------------------------------------------ взаимодействие
    public static void use(ServerPlayer p, String id) {
        StoryData d = Progress.d(p);
        if (id.equals("hub") || id.startsWith("home:")) {
            if (d.apartment < 0) { Story.say(p, "Врата", 0x8FE3FF, "Врата общежития откликаются только жильцам — сначала заселись."); return; }
            open(p, "home");
            return;
        }
        Gate g = byId(id);
        if (g == null) return;
        if (!found(d, id)) { d.gates.putInt(id, 1); p.setData(ModRegistry.STORY.get(), d); }
        if (fixed(d, id)) { open(p, id); return; }
        // починка
        StringBuilder need = new StringBuilder();
        for (Cost c : g.costs) need.append(c.item.get().getDescription().getString()).append("\u00A0×").append(c.n).append(", ");
        need.append(g.money).append(" ✦");
        DialogueManager.Dialogue dl = new DialogueManager.Dialogue("a")
                .node("a", "Врата «" + g.name + "»", 0x8FE3FF, "Кристалл треснул и лежит на камне. Чтобы врата заработали, нужно: " + need + ".",
                        DialogueManager.opt("Починить", "fix"), DialogueManager.opt("[Уйти]", null))
                .node("fix", "Врата «" + g.name + "»", 0x8FE3FF, "…", pp -> repair(pp, g), DialogueManager.opt("[Закрыть]", null));
        DialogueManager.open(p, null, dl);
    }

    private static void repair(ServerPlayer p, Gate g) {
        StoryData d = Progress.d(p);
        for (Cost c : g.costs) if (Bag.count(p, s -> s.is(c.item.get())) < c.n) {
            p.sendSystemMessage(Component.literal("Не хватает: " + c.item.get().getDescription().getString() + " ×" + c.n + ".").withStyle(ChatFormatting.RED));
            return;
        }
        if (d.money < g.money) { p.sendSystemMessage(Component.literal("Не хватает Искр: " + g.money + " ✦.").withStyle(ChatFormatting.RED)); return; }
        for (Cost c : g.costs) Bag.take(p, s -> s.is(c.item.get()), c.n);
        d.money -= g.money;
        d.gates.putInt(g.id, 2);
        p.setData(ModRegistry.STORY.get(), d);
        Story.sync(p);
        sync(p);
        Vec3 at = SPOTS.getOrDefault(g.id, p.position());
        p.serverLevel().sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1.3, at.z, 60, 0.4, 0.6, 0.4, 0.08);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.2f);
        Story.title(p, "Врата починены", g.name + " — перенос бесплатный", 0x8FE3FF);
    }

    /** Окно врат: from = id врат или "home". */
    public static void open(ServerPlayer p, String from) {
        StoryData d = Progress.d(p);
        com.google.gson.JsonObject j = new com.google.gson.JsonObject();
        j.addProperty("from", from);
        j.addProperty("home", home(d));
        j.addProperty("lift", d.liftOk);
        j.addProperty("money", d.money);
        j.addProperty("cd", Math.max(0, (CD.getOrDefault(p.getUUID(), 0L) - System.currentTimeMillis()) / 1000));
        com.google.gson.JsonObject st = new com.google.gson.JsonObject();
        for (Gate g : ALL) st.addProperty(g.id, d.gates.getInt(g.id));
        j.add("st", st);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(7, j.toString()));
    }

    /** Список починенных — клиенту для анимации врат. */
    public static void sync(ServerPlayer p) {
        StoryData d = Progress.d(p);
        com.google.gson.JsonArray a = new com.google.gson.JsonArray();
        for (Gate g : ALL) if (fixed(d, g.id)) a.add(g.id);
        com.google.gson.JsonObject j = new com.google.gson.JsonObject();
        j.add("fixed", a);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(8, j.toString()));
    }

    /** Команда из окна: перенестись к to (id врат или "home"). from — где стоит игрок. */
    public static void request(ServerPlayer p, String from, String to) {
        StoryData d = Progress.d(p);
        long cd = CD.getOrDefault(p.getUUID(), 0L) - System.currentTimeMillis();
        if (cd > 0) { msg(p, "Врата ещё не остыли: " + (cd / 1000 + 1) + " с.", ChatFormatting.RED); return; }
        if (Vampire.jailed(p)) { msg(p, "Из темницы врата не ведут.", ChatFormatting.RED); return; }
        if (!to.equals("home")) {
            Gate g = byId(to);
            if (g == null || !fixed(d, to)) { msg(p, "Эти врата ещё не починены.", ChatFormatting.RED); return; }
            if (from.equals("home") ? g.floor > home(d) || (g.floor == 2 && !d.liftOk) : byId(from) == null || byId(from).floor != g.floor) {
                msg(p, from.equals("home") ? "Врата общежития пока не достают до этого этажа — улучши их." : "С этих врат — только по своему этажу и домой.", ChatFormatting.RED);
                return;
            }
        }
        if (from.equals("home")) {
            CivData.Apartment a = CivData.get(p.server).byId(d.apartment);
            if (a == null || p.level().dimension() != CivWorld.WORLD1 || p.position().distanceToSqr(hubSpot(p.server)) > 49) { msg(p, "Подойди к вратам в атриуме общежития.", ChatFormatting.RED); return; }
        } else {
            Gate fg = byId(from);
            Vec3 at = fg == null ? null : SPOTS.get(fg.id);
            if (at == null || p.position().distanceToSqr(at) > 36) { msg(p, "Подойди к вратам.", ChatFormatting.RED); return; }
        }
        CHANNEL.put(p.getUUID(), new Channel(p.position(), to));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.4f, 1.6f);
    }

    private static void go(ServerPlayer p, String to) {
        StoryData d = Progress.d(p);
        ServerLevel l; Vec3 at;
        if (to.equals("home")) {
            CivData.Apartment a = CivData.get(p.server).byId(d.apartment);
            if (a == null) return;
            l = CivWorld.level(p.server); Vec3 h = hubSpot(p.server);
            at = Taxi.safe(l, BlockPos.containing(h.x, h.y, h.z + 3));
        } else {
            Gate g = byId(to);
            l = lvl(p.server, g.floor);
            Vec3 s = SPOTS.computeIfAbsent(g.id, k -> spot(p.server, g));
            at = Taxi.safe(l, BlockPos.containing(s.x + 2, s.y, s.z));
        }
        CD.put(p.getUUID(), System.currentTimeMillis() + 30_000);
        p.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 60, 0.4, 0.9, 0.4, 0.1);
        PacketDistributor.sendToPlayer(p, new Network.Cinematic(6, 40, ""));
        p.teleportTo(l, at.x, at.y, at.z, p.getYRot(), 0);
        p.resetFallDistance();
        l.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1, at.z, 50, 0.4, 0.9, 0.4, 0.06);
        l.playSound(null, BlockPos.containing(at), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.2f);
    }

    /** Улучшить домашние врата до следующего этажа. */
    public static void upgradeHome(ServerPlayer p) {
        StoryData d = Progress.d(p);
        int next = home(d) + 1;
        if (next >= HOME_UP.length) { msg(p, "Дальше этажей пока нет.", ChatFormatting.GRAY); return; }
        if (next == 2 && !d.liftOk) { msg(p, "Сначала открой этаж 2 (лифт и Луна).", ChatFormatting.RED); return; }
        for (Cost c : HOME_UP[next]) if (Bag.count(p, s -> s.is(c.item.get())) < c.n) { msg(p, "Не хватает: " + c.item.get().getDescription().getString() + " ×" + c.n + ".", ChatFormatting.RED); return; }
        if (d.money < HOME_UP_MONEY[next]) { msg(p, "Не хватает Искр: " + HOME_UP_MONEY[next] + " ✦.", ChatFormatting.RED); return; }
        for (Cost c : HOME_UP[next]) Bag.take(p, s -> s.is(c.item.get()), c.n);
        d.money -= HOME_UP_MONEY[next];
        d.homeGate = next;
        p.setData(ModRegistry.STORY.get(), d);
        Story.sync(p);
        Story.title(p, "Врата общежития: уровень " + next, "Теперь достают до этажа " + next, 0x8FE3FF);
        open(p, "home");
    }

    public static String costText(Cost[] cs, int money) {
        StringBuilder sb = new StringBuilder();
        for (Cost c : cs) sb.append(c.item.get().getDescription().getString()).append("\u00A0×").append(c.n).append(", ");
        return sb.append(money).append(" ✦").toString();
    }

    public static void forget(ServerPlayer p) { CHANNEL.remove(p.getUUID()); }

    private static void msg(ServerPlayer p, String s, ChatFormatting c) { p.sendSystemMessage(Component.literal(s).withStyle(c)); }
}
