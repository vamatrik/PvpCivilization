package com.pvpciv.game;

import net.minecraft.resources.ResourceLocation;
import com.pvpciv.combat.Mobs;
import com.pvpciv.data.StoryData;
import com.pvpciv.entity.boss.VoidDemon;
import com.pvpciv.loot.LootTables;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModF5;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.CivWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/**
 * 1.0 — фаза 2 финала: разлом Пустоты и демоница с полосой маны вместо здоровья.
 * Червь пал → взрыв, тьма, всех бойцов переносит сюда. Сначала над полом появляются платформы («залезь!»), затем пол исчезает.
 * Мана тратится по стадиям (по 25%):
 *  1. «Цветной пол» — демоница называет цвет, остальные плиты исчезают. Не упади в космос.
 *  2. «Лазеры» — лучи бегут по полу: перепрыгивай. Сначала по одному, потом по три.
 *  3. «Паркур на время» — платформы до золотой цели (жёлтая кромка — «прыгай отсюда»). Каждый финиш выжигает ману.
 *  4. «Волны» — неуязвимые мобы; продержись.
 * Мана кончилась — демоница уходит в Ад и оставляет «адский след» (ремонт лифта станции) и награду.
 * Падение — −30% здоровья и назад на арену (на паркуре — к старту, −15%). Смерть — выбываешь.
 */
public final class VoidRift {
    private VoidRift() {}
    public static final BlockPos C = new BlockPos(0, 150, 1600);
    private static final int R = 14, GRID = 10, TILE = 4;
    private static final Block[] COLORS = {Blocks.RED_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE};
    private static final String[] COLOR_NAMES = {"КРАСНЫЙ", "СИНИЙ", "ЖЁЛТЫЙ", "ЗЕЛЁНЫЙ"};
    private static final ChatFormatting[] COLOR_FMT = {ChatFormatting.RED, ChatFormatting.BLUE, ChatFormatting.YELLOW, ChatFormatting.GREEN};

    private static boolean active;
    private static int t, stage, sub, round, demonLevel = 38;
    private static float mana;
    private static UUID demon;
    private static final Set<UUID> FIGHTERS = new LinkedHashSet<>();
    private static ServerBossEvent bar;
    private static final int[][] grid = new int[GRID][GRID];
    private static int safeColor = -1;
    private static boolean tilesOff;
    private static final List<Laser> LASERS = new ArrayList<>();
    private static final Map<UUID, Long> LASER_CD = new HashMap<>();
    private static final List<BlockPos> PATH = new ArrayList<>();
    private static final Map<UUID, Integer> LAP_T = new HashMap<>();
    private static final Set<UUID> FINISHED = new HashSet<>();
    private static final List<UUID> MOBS = new ArrayList<>();
    private static int endT = -1;
    private static long started;
    private static final Map<UUID, List<ItemStack>> PENDING = new HashMap<>();

    public static boolean active() { return active; }
    /** 1.0.1: музыка разлома — бойцам. */
    public static void music(MinecraftServer server, Map<UUID, String[]> out) {
        if (!active) return;
        long gt = server.overworld().getGameTime();
        for (UUID u : FIGHTERS) out.put(u, new String[]{"demon", String.valueOf((gt - started) / 20.0)});
    }
    /** Отладка: /civ admin rift <мана>. */
    public static void setMana(ServerPlayer p, int m) {
        if (!active) { debug = true; begin(p.serverLevel(), List.of(p.getUUID())); debug = false; }
        mana = m;
    }
    private static boolean debug;
    public static boolean inRift(ServerPlayer p) { return active && FIGHTERS.contains(p.getUUID()); }

    // ------------------------------------------------------------------ старт
    public static void begin(ServerLevel belt, List<UUID> who) {
        if (active) return;
        MinecraftServer s = belt.getServer();
        FIGHTERS.clear();
        for (UUID u : who) {
            ServerPlayer p = s.getPlayerList().getPlayer(u);
            if (p != null && p.isAlive() && p.level() == belt && (debug || p.position().distanceTo(Vec3.atCenterOf(com.pvpciv.floor.Belt.CENTER)) < 90)) FIGHTERS.add(u);
        }
        if (FIGHTERS.isEmpty()) return;
        active = true; t = 0; stage = 0; sub = 0; round = 0; mana = 100; endT = -1;
        started = s.overworld().getGameTime();
        LASERS.clear(); PATH.clear(); LAP_T.clear(); FINISHED.clear(); MOBS.clear();
        bar = new ServerBossEvent(Component.literal("Демоница Пустоты · мана"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_20);
        bar.setProgress(1f);
        for (UUID u : FIGHTERS) {
            ServerPlayer p = s.getPlayerList().getPlayer(u);
            PacketDistributor.sendToPlayer(p, new Network.PhoneData(22, "{\"cut\":\"rift\",\"len\":50}"));
        }
    }

    private static List<ServerPlayer> fighters(MinecraftServer s) {
        List<ServerPlayer> out = new ArrayList<>();
        for (UUID u : FIGHTERS) { ServerPlayer p = s.getPlayerList().getPlayer(u); if (p != null) out.add(p); }
        return out;
    }

    private static void enter(ServerLevel l) {
        clearArena(l);
        disc(l, C.getY() - 1, Blocks.BLACK_CONCRETE.defaultBlockState(), Blocks.PURPLE_STAINED_GLASS.defaultBlockState());
        int i = 0;
        List<ServerPlayer> fs = fighters(l.getServer());
        for (ServerPlayer p : fs) {
            double a = Math.PI * 2 * i++ / Math.max(1, fs.size());
            p.teleportTo(l, C.getX() + 0.5 + Math.cos(a) * 9, C.getY(), C.getZ() + 0.5 + Math.sin(a) * 9, 0, 0);
            p.resetFallDistance();
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("ФАЗА 2").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Демоница Пустоты").withStyle(ChatFormatting.LIGHT_PURPLE)));
            PacketDistributor.sendToPlayer(p, new Network.PhoneData(22, "{\"cut\":\"arrive\",\"len\":30}"));
            bar.addPlayer(p);
        }
        VoidDemon d = com.pvpciv.registry.ModMobs.VOID_DEMON.get().create(l);
        if (d != null) {
            d.moveTo(C.getX() + 0.5, C.getY(), C.getZ() + 0.5, 180, 0);
            Mobs.setup(d, demonLevel, false, "Демоница Пустоты", "none");
            d.setCustomName(Component.literal("☠ Демоница Пустоты").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            d.setCustomNameVisible(true);
            l.addFreshEntity(d);
            demon = d.getUUID();
        }
        say(l, "«Ты убил моего червячка? Как грубо. Поиграем — пока у меня не кончится мана.»");
    }

    // ------------------------------------------------------------------ ход
    public static void tick(MinecraftServer server) {
        gravSync(server);
        if (!active) return;
        ServerLevel l = CivWorld.f5(server);
        t++;
        if (t == 45) { enter(l); return; }
        if (t < 45) return;
        // кто ещё в бою
        FIGHTERS.removeIf(u -> {
            ServerPlayer p = server.getPlayerList().getPlayer(u);
            boolean gone = p == null || !p.isAlive() || p.level() != l || p.position().distanceTo(Vec3.atCenterOf(C)) > 70;
            if (gone && p != null) bar.removePlayer(p);
            return gone;
        });
        if (FIGHTERS.isEmpty()) { fail(l); return; }
        VoidDemon d = demon == null ? null : (l.getEntity(demon) instanceof VoidDemon vd ? vd : null);
        if (endT >= 0) { finishTick(l, d); return; }
        for (ServerPlayer p : fighters(server)) fallCheck(l, p);
        bar.setProgress(Math.max(0, mana / 100f));
        // стадии
        if (stage == 0) {
            if (t == 46) introPlatforms(l, d);
            if (t == 46 + 160) { removeIntroFloor(l); }
            if (t == 46 + 200) { stage = 1; setupStage(l, d); }
            return;
        }
        int want = Math.min(4, 1 + (int) ((100 - mana) / 25f));
        if (mana <= 0) { win(l, d); return; }
        if (want != stage) { stage = want; setupStage(l, d); }
        sub++;
        switch (stage) {
            case 1 -> { mana -= 25f / (80 * 20); colorFloor(l, d); }
            case 2 -> { mana -= 25f / (80 * 20); lasers(l, d); }
            case 3 -> { mana -= 25f / (300 * 20); parkour(l, d); }
            case 4 -> { mana -= 25f / (70 * 20); waves(l, d); }
            default -> {}
        }
        if (d != null) {
            Vec3 at = Vec3.atBottomCenterOf(C).add(Math.cos(t * 0.02) * 2, stage == 3 ? 14 : 4 + Math.sin(t * 0.05), Math.sin(t * 0.02) * 2);
            d.teleportTo(at.x, at.y, at.z);
        }
    }

    private static void setupStage(ServerLevel l, VoidDemon d) {
        sub = 0;
        clearMobs(l);
        LASERS.clear();
        if (d != null) d.anim("cast");
        l.playSound(null, C, SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 3f, 1.4f);
        String[] names = {"", "ЦВЕТНОЙ ПОЛ", "ЛАЗЕРЫ", "ПАРКУР", "ВОЛНЫ"};
        String[] hints = {"", "стой на плите названного цвета", "низкие лучи — прыгай, высокие (белые) — пригнись", "низкая гравитация · голубая кромка — нужен рывок · фонари — чекпоинты", "мобы неуязвимы — продержись"};
        for (ServerPlayer p : fighters(l.getServer())) {
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(names[stage]).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Стадия " + stage + "/4 · " + hints[stage]).withStyle(ChatFormatting.GRAY)));
        }
        clearLayer(l, C.getY());
        clearPath(l);
        HALF = false;
        switch (stage) {
            case 1 -> { round = 0; rollGrid(); drawGrid(l, -1); say(l, "«Любимые цвета? У меня — все. Но стоять можно только на одном.»"); }
            case 2 -> { disc(l, C.getY(), Blocks.BLACK_CONCRETE.defaultBlockState(), Blocks.MAGENTA_STAINED_GLASS.defaultBlockState()); say(l, "«Попрыгаем? Скакалку я принесла.»"); }
            case 3 -> { buildPath(l); for (ServerPlayer p : fighters(l.getServer())) toStart(p); FINISHED.clear(); say(l, "«Бегом! Кто последний — тот мой.»"); }
            case 4 -> { disc(l, C.getY(), Blocks.OBSIDIAN.defaultBlockState(), Blocks.CRYING_OBSIDIAN.defaultBlockState()); for (ServerPlayer p : fighters(l.getServer())) p.teleportTo(C.getX() + 0.5, C.getY() + 1, C.getZ() + 0.5); say(l, "«Мои детки бессмертны. А ты?»"); }
            default -> {}
        }
    }

    // ---- вступление: платформы над полом, затем пол исчезает
    private static void introPlatforms(ServerLevel l, VoidDemon d) {
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + Math.PI / 2 * k;
            int cx = C.getX() + (int) Math.round(Math.cos(a) * 7), cz = C.getZ() + (int) Math.round(Math.sin(a) * 7);
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) l.setBlock(new BlockPos(cx + x, C.getY(), cz + z), Blocks.AMETHYST_BLOCK.defaultBlockState(), 2);
            l.sendParticles(ParticleTypes.END_ROD, cx + 0.5, C.getY() + 1.2, cz + 0.5, 20, 1, 0.3, 1, 0.02);
        }
        for (ServerPlayer p : fighters(l.getServer())) {
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("НА ПЛАТФОРМЫ!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Залезь повыше — пол сейчас исчезнет").withStyle(ChatFormatting.GRAY)));
        }
        if (d != null) d.anim("laugh");
        l.playSound(null, C, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 3f, 0.7f);
    }

    private static void removeIntroFloor(ServerLevel l) {
        clearLayer(l, C.getY() - 1);
        l.playSound(null, C, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3f, 0.5f);
        l.sendParticles(ParticleTypes.REVERSE_PORTAL, C.getX() + 0.5, C.getY() - 1, C.getZ() + 0.5, 200, R / 2.0, 0.3, R / 2.0, 0.1);
    }

    // ---- стадия 1: цветной пол
    private static void rollGrid() { Random r = new Random(); for (int[] row : grid) for (int j = 0; j < GRID; j++) row[j] = r.nextInt(COLORS.length); }

    private static void drawGrid(ServerLevel l, int onlyColor) {
        int half = GRID * TILE / 2;
        for (int gx = 0; gx < GRID; gx++) for (int gz = 0; gz < GRID; gz++) {
            boolean on = onlyColor < 0 || grid[gx][gz] == onlyColor;
            BlockState s = on ? COLORS[grid[gx][gz]].defaultBlockState() : Blocks.AIR.defaultBlockState();
            for (int x = 0; x < TILE; x++) for (int z = 0; z < TILE; z++)
                l.setBlock(new BlockPos(C.getX() - half + gx * TILE + x, C.getY(), C.getZ() - half + gz * TILE + z), s, 2);
        }
    }

    private static void colorFloor(ServerLevel l, VoidDemon d) {
        int announce = Math.max(28, 70 - round * 6), off = 40, rest = 24;
        int cyc = sub % (announce + off + rest);
        if (cyc == 0) {
            safeColor = new Random().nextInt(COLORS.length);
            for (ServerPlayer p : fighters(l.getServer())) {
                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(COLOR_NAMES[safeColor]).withStyle(COLOR_FMT[safeColor], ChatFormatting.BOLD)));
                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("встань на этот цвет").withStyle(ChatFormatting.GRAY)));
            }
            if (d != null) d.anim("point");
            l.playSound(null, C, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 3f, 0.6f + safeColor * 0.2f);
        } else if (cyc == announce) {
            drawGrid(l, safeColor);
            tilesOff = true;
            l.playSound(null, C, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3f, 0.8f);
        } else if (cyc == announce + off) {
            rollGrid();
            drawGrid(l, -1);
            tilesOff = false;
            round++;
        }
    }

    // ---- стадия 2: лазеры (низкие — прыгать, высокие белые — приседать; к концу — чаще, быстрее, крест-накрест)
    private record Laser(boolean alongX, double pos, double speed, boolean high) {}

    private static void lasers(ServerLevel l, VoidDemon d) {
        float lin = Math.max(0, Math.min(1, (75 - mana) / 25f));   // 0 в начале стадии → 1 в конце
        float frac = (float) Math.sqrt(lin);   // 1.1.1: сложность растёт быстро в начале и почти не растёт к концу
        int maxN = 2 + Math.round(frac * 4);
        int every = Math.max(14, 54 - (int) (frac * 40));
        Random rnd = new Random();
        if (sub % every == 0 && LASERS.size() < maxN) {
            int burst = frac > 0.75f && rnd.nextFloat() < 0.5f ? 2 : 1;
            boolean alongX = rnd.nextBoolean();
            for (int k = 0; k < burst; k++) {
                boolean ax = burst == 2 ? (k == 0) : alongX, fromNeg = rnd.nextBoolean();
                boolean high = frac > 0.3f && rnd.nextFloat() < 0.25f + frac * 0.2f;
                double speed = (0.2 + frac * 0.2) * (fromNeg ? 1 : -1);
                LASERS.add(new Laser(ax, fromNeg ? -R - 1 : R + 1, speed, high));
            }
            if (d != null) d.anim("point");
            l.playSound(null, C, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2f, 1.6f);
        }
        for (int i = LASERS.size() - 1; i >= 0; i--) {
            Laser z = LASERS.get(i);
            Laser n = new Laser(z.alongX, z.pos + z.speed, z.speed, z.high);
            if (Math.abs(n.pos) > R + 2) { LASERS.remove(i); continue; }
            LASERS.set(i, n);
            double y = C.getY() + (z.high ? 2.65 : 1.35);
            Vec3 a = z.alongX ? new Vec3(C.getX() + 0.5 + n.pos, y, C.getZ() - R) : new Vec3(C.getX() - R, y, C.getZ() + 0.5 + n.pos);
            Vec3 b = z.alongX ? new Vec3(C.getX() + 0.5 + n.pos, y, C.getZ() + R + 1) : new Vec3(C.getX() + R + 1, y, C.getZ() + 0.5 + n.pos);
            if (sub % 2 == 0) line(l, a, b, z.high ? 0xFFF0FF : 0xFF30E0, z.high ? 0.16f : 0.12f, 140);
            for (ServerPlayer p : fighters(l.getServer())) {
                double along = z.alongX ? p.getX() - C.getX() - 0.5 : p.getZ() - C.getZ() - 0.5;
                if (Math.abs(along - n.pos) > 0.55 || p.getY() < C.getY()) continue;
                boolean hit = z.high ? (p.getY() < y && p.getY() + p.getBbHeight() > y) : p.getY() <= y - 0.1;
                if (!hit) continue;
                long now = l.getGameTime();
                if (LASER_CD.getOrDefault(p.getUUID(), 0L) > now) continue;
                LASER_CD.put(p.getUUID(), now + 12);
                p.invulnerableTime = 0;
                p.hurt(l.damageSources().magic(), p.getMaxHealth() * (0.18f + frac * 0.1f));
                p.setDeltaMovement(p.getDeltaMovement().add(0, z.high ? -0.2 : 0.5, 0));
                p.hurtMarked = true;
                l.sendParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 0.5, p.getZ(), 1, 0, 0, 0, 0);
            }
        }
    }

    private static void line(ServerLevel l, Vec3 a, Vec3 b, int color, float w, int life) {
        String js = String.format(Locale.ROOT, "{\"a\":[%.3f,%.3f,%.3f],\"b\":[%.3f,%.3f,%.3f],\"c\":%d,\"w\":%.3f,\"l\":%d,\"z\":true}", a.x, a.y, a.z, b.x, b.y, b.z, color, w, life);
        for (ServerPlayer p : fighters(l.getServer())) PacketDistributor.sendToPlayer(p, new Network.PhoneData(16, js));
    }

    // ---- стадия 3: длинный паркур-спираль вверх; низкая гравитация, участки под рывок, чекпоинты
    private static final int LAP = 150 * 20;
    private static final List<BlockPos> PATH_BLOCKS = new ArrayList<>();
    private static final List<Integer> CHECKS = new ArrayList<>();          // индексы платформ-чекпоинтов
    private static final Map<UUID, Integer> AT_CHECK = new HashMap<>();
    private static final ResourceLocation RIFT_GRAV = com.pvpciv.PvpCiv.id("rift_gravity");

    private static void put(ServerLevel l, BlockPos q, Block b) { l.setBlock(q, b.defaultBlockState(), 2); PATH_BLOCKS.add(q); }

    private static void buildPath(ServerLevel l) {
        clearPath(l);
        PATH.clear(); CHECKS.clear(); AT_CHECK.clear();
        Random r = new Random(1001);
        double ang = 0, rad = 20; int y = C.getY();
        BlockPos start = at(ang, rad, y);
        platform(l, start, 2, Blocks.WARPED_WART_BLOCK);
        PATH.add(start); CHECKS.add(0);
        int sinceDash = 0;
        for (int i = 1; i <= 30; i++) {
            boolean dash = sinceDash >= 4 && i % 5 == 0 && i < 30;
            boolean check = i % 8 == 0;
            int dy = dash ? 0 : r.nextInt(3);                        // 0..+2 (в низкой гравитации — спокойно)
            double gap = dash ? 9 : dy == 2 ? 3.5 : 4 + r.nextInt(2);   // от кромки до кромки (3×3 платформа → +3); подъём — короче
            double nrad = Math.max(16, Math.min(24, rad + (r.nextInt(3) - 1) * 2));
            BlockPos prev = PATH.get(PATH.size() - 1);
            // кромка у края прошлой платформы в сторону следующей
            double nang = ang + (gap + 3) / ((rad + nrad) / 2);
            BlockPos next = at(nang, nrad, y + dy);
            Vec3 dir = new Vec3(next.getX() - prev.getX(), 0, next.getZ() - prev.getZ()).normalize();
            BlockPos edge = prev.offset((int) Math.round(dir.x * 1.4), 0, (int) Math.round(dir.z * 1.4));
            Block edgeB = dash ? Blocks.LIGHT_BLUE_STAINED_GLASS : Blocks.GLOWSTONE;
            put(l, edge, edgeB);
            if (dash) { put(l, edge.offset(dir.z > 0.5 || dir.z < -0.5 ? 1 : 0, 0, dir.x > 0.5 || dir.x < -0.5 ? 1 : 0), edgeB); sinceDash = 0; } else sinceDash++;
            ang = nang; rad = nrad; y += dy;
            platform(l, next, check ? 2 : 1, check ? Blocks.SEA_LANTERN : Blocks.PURPUR_BLOCK);
            PATH.add(next);
            if (check) CHECKS.add(PATH.size() - 1);
        }
        double nang = ang + 7.0 / rad;
        BlockPos goal = at(nang, rad, y + 1);
        platform(l, goal, 2, Blocks.GOLD_BLOCK);
        PATH.add(goal);
    }

    private static BlockPos at(double ang, double rad, int y) { return new BlockPos(C.getX() + (int) Math.round(Math.cos(ang) * rad), y, C.getZ() + (int) Math.round(Math.sin(ang) * rad)); }

    private static void platform(ServerLevel l, BlockPos c, int r, Block b) {
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) put(l, c.offset(x, 0, z), b);
    }

    private static void clearPath(ServerLevel l) {
        for (BlockPos q : PATH_BLOCKS) if (!l.getBlockState(q).isAir()) l.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
        PATH_BLOCKS.clear();
    }

    private static void toStart(ServerPlayer p) { AT_CHECK.put(p.getUUID(), 0); toCheck(p); LAP_T.put(p.getUUID(), 0); }

    private static void toCheck(ServerPlayer p) {
        if (PATH.isEmpty()) return;
        BlockPos s = PATH.get(CHECKS.isEmpty() ? 0 : CHECKS.get(AT_CHECK.getOrDefault(p.getUUID(), 0)));
        BlockPos nx = PATH.get(Math.min(PATH.size() - 1, PATH.indexOf(s) + 1));
        float yaw = (float) (Math.toDegrees(Math.atan2(nx.getZ() - s.getZ(), nx.getX() - s.getX())) - 90);
        p.teleportTo(s.getX() + 0.5, s.getY() + 1, s.getZ() + 0.5);
        p.connection.teleport(p.getX(), p.getY(), p.getZ(), yaw, 0);
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
    }

    private static void gravSync(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            var g = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY);
            if (g == null) continue;
            boolean want = active && stage == 3 && endT < 0 && FIGHTERS.contains(p.getUUID());
            boolean on = g.getModifier(RIFT_GRAV) != null;
            if (want && !on) g.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(RIFT_GRAV, -0.5, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            else if (!want && on) g.removeModifier(RIFT_GRAV);
            if (want) p.fallDistance = Math.min(p.fallDistance, 2f);
        }
    }

    private static void parkour(ServerLevel l, VoidDemon d) {
        if (PATH.isEmpty()) return;
        BlockPos goal = PATH.get(PATH.size() - 1);
        int n = Math.max(1, FIGHTERS.size());
        for (ServerPlayer p : fighters(l.getServer())) {
            int lt = LAP_T.merge(p.getUUID(), 1, Integer::sum);
            int ci = AT_CHECK.getOrDefault(p.getUUID(), 0);
            // чекпоинты
            for (int k = ci + 1; k < CHECKS.size(); k++) {
                BlockPos c = PATH.get(CHECKS.get(k));
                if (Math.abs(p.getX() - c.getX() - 0.5) < 2.6 && Math.abs(p.getZ() - c.getZ() - 0.5) < 2.6 && Math.abs(p.getY() - c.getY() - 1) < 1.5) {
                    AT_CHECK.put(p.getUUID(), k); ci = k;
                    p.connection.send(new ClientboundSetActionBarTextPacket(Component.literal("✔ Чекпоинт " + k + "/" + (CHECKS.size() - 1)).withStyle(ChatFormatting.AQUA)));
                    l.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.5f);
                }
            }
            // падение ниже текущего чекпоинта
            int floorY = PATH.get(CHECKS.get(ci)).getY();
            if (p.getY() < floorY - 7) {
                p.hurt(l.damageSources().magic(), p.getMaxHealth() * 0.08f);
                toCheck(p);
                continue;
            }
            if (lt % 20 == 0) p.connection.send(new ClientboundSetActionBarTextPacket(Component.literal("Время: " + Math.max(0, (LAP - lt) / 20) + " с · чекпоинт " + ci + "/" + (CHECKS.size() - 1) + " · финишей: " + FINISHED.size() + "/" + n).withStyle(ChatFormatting.GOLD)));
            if (Math.abs(p.getX() - goal.getX() - 0.5) < 2.6 && Math.abs(p.getZ() - goal.getZ() - 0.5) < 2.6 && Math.abs(p.getY() - goal.getY() - 1) < 1.5) {
                mana -= 25f / n * (FINISHED.add(p.getUUID()) ? 1f : 0.5f);
                l.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.4f);
                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("ФИНИШ!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
                if (d != null) d.anim("cast");
                toStart(p);
            } else if (lt >= LAP) {
                p.hurt(l.damageSources().magic(), p.getMaxHealth() * 0.15f);
                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("ВРЕМЯ!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
                toStart(p);
            }
        }
    }

    // ---- стадия 4: волны неуязвимых — тени мобов со всех этажей, много стрелков; с середины — ускорение
    private static boolean HALF;
    private static final Mobs.Kind[] RANGED = {Mobs.Kind.ASH_ARCHER, Mobs.Kind.FROST_ARCHER, Mobs.Kind.GUNNER, Mobs.Kind.ASHIGARU, Mobs.Kind.KITSUNE_ARCHER, Mobs.Kind.LAB_TECH, Mobs.Kind.POLICE};
    private static final Mobs.Kind[] MELEE = {Mobs.Kind.XENO, Mobs.Kind.VOID_CRAWLER, Mobs.Kind.ROGUE_ASTRO, Mobs.Kind.WEREWOLF, Mobs.Kind.CHIMERA, Mobs.Kind.GHOUL, Mobs.Kind.FROST_SPIDER};

    private static void waves(ServerLevel l, VoidDemon d) {
        if (!HALF && mana <= 12.5f) {
            HALF = true;
            for (UUID u : MOBS) if (l.getEntity(u) instanceof Mob m) haste(m);
            say(l, "«Скучно. Детки, побыстрее!»");
            for (ServerPlayer p : fighters(l.getServer())) p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("ТЕНИ УСКОРИЛИСЬ").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
            l.playSound(null, C, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2f, 1.6f);
        }
        if (sub % (HALF ? 130 : 160) != 1) return;
        Random r = new Random();
        int n = 2 + FIGHTERS.size() + sub / 300;
        for (int i = 0; i < n; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            boolean ranged = r.nextFloat() < 0.45f;
            double dist = ranged ? R - 1 : R - 3;
            Vec3 at = new Vec3(C.getX() + 0.5 + Math.cos(a) * dist, C.getY() + 1, C.getZ() + 0.5 + Math.sin(a) * dist);
            Mobs.Kind k = ranged ? RANGED[r.nextInt(RANGED.length)] : MELEE[r.nextInt(MELEE.length)];
            Mob m = k.type().create(l);
            if (m == null) continue;
            m.moveTo(at.x, at.y, at.z, 0, 0);
            Mobs.setupDefault(m);
            Mobs.setup(m, demonLevel, false, "Тень: " + k.title, "floor5_mob");   // 1.1.1: урон — как у мобов пояса (ур. 38), без поправок вида
            m.setInvulnerable(true);
            m.addTag("boss_minion");
            m.setCustomName(Component.literal("Тень: " + k.title + " (неуязвима)").withStyle(ChatFormatting.DARK_PURPLE));
            if (HALF) haste(m);
            l.addFreshEntity(m);
            MOBS.add(m.getUUID());
            l.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1, at.z, 30, 0.4, 0.8, 0.4, 0.1);
        }
        if (d != null) d.anim("cast");
        l.playSound(null, C, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 3f, 0.8f);
    }

    private static void haste(Mob m) {
        m.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 20 * 120, 1, false, true));
        m.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SPEED, 20 * 120, 1, false, false));
    }

    private static void clearMobs(ServerLevel l) {
        for (UUID u : MOBS) { var e = l.getEntity(u); if (e != null) { l.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 1, e.getZ(), 10, 0.3, 0.5, 0.3, 0.02); e.discard(); } }
        MOBS.clear();
    }

    // ------------------------------------------------------------------ падения
    private static void fallCheck(ServerLevel l, ServerPlayer p) {
        if (p.getY() > C.getY() - 12) return;
        p.resetFallDistance();
        if (stage == 3) { p.hurt(l.damageSources().magic(), p.getMaxHealth() * 0.08f); toCheck(p); return; }
        p.hurt(l.damageSources().magic(), p.getMaxHealth() * 0.3f);
        Vec3 to = Vec3.atBottomCenterOf(C).add(0, 1, 0);
        if (stage == 0) to = new Vec3(C.getX() + 0.5 + 5, C.getY() + 1, C.getZ() + 0.5 + 5);
        if (stage == 1 && tilesOff && safeColor >= 0) {
            int half = GRID * TILE / 2;
            outer:
            for (int gx = 0; gx < GRID; gx++) for (int gz = 0; gz < GRID; gz++) if (grid[gx][gz] == safeColor) {
                to = new Vec3(C.getX() - half + gx * TILE + TILE / 2.0, C.getY() + 1, C.getZ() - half + gz * TILE + TILE / 2.0);
                break outer;
            }
        }
        p.teleportTo(to.x, to.y, to.z);
        p.displayClientMessage(Component.literal("Пустота выплюнула тебя обратно: −30% здоровья").withStyle(ChatFormatting.RED), true);
    }

    public static void onDemonHit(ServerPlayer p, float amount) {
        if (!active || stage == 0 || endT >= 0) return;
        mana -= 0.12f;
        p.serverLevel().sendParticles(ParticleTypes.WITCH, p.getX(), p.getY() + 1, p.getZ(), 4, 0.3, 0.3, 0.3, 0);
    }

    // ------------------------------------------------------------------ конец
    private static void win(ServerLevel l, VoidDemon d) {
        endT = 0;
        clearMobs(l);
        LASERS.clear();
        bar.setProgress(0);
        if (d != null) d.anim("laugh");
        say(l, "«Мана кончилась… Ладно, ладно. Увидимся внизу — в Аду. Там ты мне за всё заплатишь.»");
        l.playSound(null, C, SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 3f, 1.4f);
        // 1.1.1: никакого телепорта — взрыв, игроки зависают в пустоте (клетки из барьера), сюжет, лут падает к ним в клетку
        clearPath(l);
        clearLayer(l, C.getY()); clearLayer(l, C.getY() + 1);
        MinecraftServer s = l.getServer();
        CAGES.clear();
        int ci = 0;
        List<ServerPlayer> fs = fighters(s);
        for (ServerPlayer p : fs) {
            double a = Math.PI * 2 * ci++ / Math.max(1, fs.size());
            BlockPos cg = BlockPos.containing(C.getX() + 0.5 + (fs.size() > 1 ? Math.cos(a) * 5 : 0), C.getY() + 3, C.getZ() + 4.5 + (fs.size() > 1 ? Math.sin(a) * 5 : 0));
            cage(l, cg, true);
            CAGES.put(p.getUUID(), cg);
            p.teleportTo(cg.getX() + 0.5, cg.getY(), cg.getZ() + 0.5);
            p.setDeltaMovement(Vec3.ZERO);
            p.resetFallDistance();
            StoryData sd = Progress.d(p);
            boolean first = (sd.trophies & 512) == 0;
            List<ItemStack> loot = new ArrayList<>();
            LootTables.PREFER = Classes.prefer(p);
            try { loot.addAll(LootTables.roll("floor5_final_personal", l.random, 1.0)); } finally { LootTables.PREFER = null; }
            loot.add(new ItemStack(ModF5.HELL_TRAIL.get()));
            if (first) {
                sd.trophies |= 512;
                p.setData(ModRegistry.STORY.get(), sd);
                loot.add(new ItemStack(ModF5.DARK_STAR.get()));
                loot.add(new ItemStack(com.pvpciv.registry.ModV11.DEMON_TROPHY.get()));
                Classes.giveMatter(p, 2);
            }
            for (ItemStack st : loot) Story.announceRareDrop(l, st, p.getGameProfile().getName());
            PENDING.put(p.getUUID(), loot);   // 1.1.1: выпадет в клетку через несколько секунд; что не подобрано — выдам на площадке
            Progress.addXp(p, Progress.xpFor(sd.level, demonLevel, 20));
            Progress.addMoney(p, 20000, true);
            com.pvpciv.progress.Titles.grant(p, "hell_walker");
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("ИЗГНАНА!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Демоница ушла в Ад. Адский след — твой: почини лифт станции").withStyle(ChatFormatting.GRAY)));
            p.sendSystemMessage(Component.literal("Награда демоницы: " + Story.lootLine(loot)).withStyle(ChatFormatting.GOLD));
        }
        // общий лут — по клеткам по кругу
        List<UUID> owners = new ArrayList<>(CAGES.keySet());
        int oi = 0;
        for (ItemStack st : LootTables.roll("floor5_final_ground", l.random, 1.0 + 0.3 * (FIGHTERS.size() - 1))) {
            Story.announceRareDrop(l, st);
            if (!owners.isEmpty()) PENDING.computeIfAbsent(owners.get(oi++ % owners.size()), k -> new ArrayList<>()).add(st);
        }
        // взрыв
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, C.getX() + 0.5 + Math.cos(a) * 6, C.getY() + 1, C.getZ() + 0.5 + Math.sin(a) * 6, 1, 0, 0, 0, 0);
        }
        l.sendParticles(ParticleTypes.DRAGON_BREATH, C.getX() + 0.5, C.getY() + 2, C.getZ() + 0.5, 300, 8, 3, 8, 0.1);
        l.playSound(null, C, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4f, 0.5f);
        l.playSound(null, C, SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 2f, 1.6f);
        for (ServerPlayer p : fs) PacketDistributor.sendToPlayer(p, new Network.PhoneData(22, "{\"cut\":\"rift\",\"len\":40}"));
        for (ServerPlayer o : s.getPlayerList().getPlayers())
            o.sendSystemMessage(Component.literal("☠ Демоница Пустоты изгнана в Ад! Галактический червь пал. Первый мир почти пройден…").withStyle(ChatFormatting.DARK_PURPLE));
    }

    private static void finishTick(ServerLevel l, VoidDemon d) {
        endT++;
        if (d != null) {
            if (endT < 60) { l.sendParticles(ParticleTypes.FLAME, d.getX(), d.getY() + 1, d.getZ(), 20, 0.5, 1, 0.5, 0.05); l.sendParticles(ParticleTypes.LAVA, d.getX(), d.getY() + 1, d.getZ(), 5, 0.5, 1, 0.5, 0); }
            if (endT == 60) { l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, d.getX(), d.getY() + 1, d.getZ(), 1, 0, 0, 0, 0); l.playSound(null, d.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.HOSTILE, 1f, 1.5f); d.discard(); demon = null; }
        }
        // 1.1.1: сюжет, пока висишь в пустоте
        String[] story = {
                "Разлом схлопывается. Пустота держит тебя, как вода — и не отпускает.",
                "Снизу поднимается жар. Там, под станцией, под всем миром, — красное зарево.",
                "☠ Демоница (эхом): «Адский след у тебя в руке — он приведёт ко мне. Почини лифт… и спускайся.»",
                "Пустота вздыхает и разжимает пальцы. Пора назад — на площадку шлюза."};
        int[] at = {60, 130, 200, 300};
        for (int i = 0; i < story.length; i++) if (endT == at[i])
            for (ServerPlayer p : fighters(l.getServer())) p.sendSystemMessage(Component.literal(story[i]).withStyle(i == 2 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (endT == 140) for (ServerPlayer p : fighters(l.getServer())) {
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Дальше — Ад").withStyle(ChatFormatting.RED)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("этаж 6 · скоро").withStyle(ChatFormatting.GRAY)));
        }
        // лут сыплется в клетку
        if (endT == 90) for (var en : CAGES.entrySet()) {
            List<ItemStack> got = PENDING.remove(en.getKey());
            if (got == null) continue;
            BlockPos cg = en.getValue();
            int k = 0;
            for (ItemStack st : got) {
                ItemEntity ie = new ItemEntity(l, cg.getX() + 0.5, cg.getY() + 2.2, cg.getZ() + 0.5, st);
                ie.setDeltaMovement(0, -0.05, 0);
                ie.setPickUpDelay(10 + k++ * 2);
                l.addFreshEntity(ie);
            }
            l.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, cg.getX() + 0.5, cg.getY() + 2, cg.getZ() + 0.5, 40, 0.6, 0.4, 0.6, 0.3);
            l.playSound(null, cg, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
        }
        if (endT % 20 == 0) for (BlockPos cg : CAGES.values()) l.sendParticles(ParticleTypes.REVERSE_PORTAL, cg.getX() + 0.5, cg.getY() - 0.5, cg.getZ() + 0.5, 12, 1.2, 0.1, 1.2, 0.02);
        if (endT >= 360) end(l);
    }

    /** Клетки игроков на финале: id → низ клетки (где стоят ноги). */
    private static final Map<UUID, BlockPos> CAGES = new HashMap<>();

    private static void cage(ServerLevel l, BlockPos c, boolean on) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = -1; y <= 3; y++) {
            boolean wall = y == -1 || y == 3 || Math.abs(x) == 1 || Math.abs(z) == 1;
            if (!wall) continue;
            BlockPos q = c.offset(x, y, z);
            l.setBlock(q, on ? Blocks.BARRIER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static void fail(ServerLevel l) {
        for (ServerPlayer o : l.getServer().getPlayerList().getPlayers())
            o.sendSystemMessage(Component.literal("☠ Демоница Пустоты смеётся: в разломе не осталось никого. Червь вернётся…").withStyle(ChatFormatting.DARK_PURPLE));
        end(l);
    }

    private static void end(ServerLevel l) {
        // неподобранное в клетках — владельцу; клетки убрать
        for (var en : CAGES.entrySet()) {
            BlockPos cg = en.getValue();
            for (ItemEntity ie : l.getEntitiesOfClass(ItemEntity.class, new AABB(cg).inflate(2, 3, 2))) {
                PENDING.computeIfAbsent(en.getKey(), k -> new ArrayList<>()).add(ie.getItem().copy());
                ie.discard();
            }
            cage(l, cg, false);
        }
        CAGES.clear();
        for (ServerPlayer p : fighters(l.getServer())) {
            BlockPos pad = com.pvpciv.floor.Belt.PAD;
            p.teleportTo(l, pad.getX() + 0.5, pad.getY(), pad.getZ() + 0.5, 180, 0);
            p.resetFallDistance();
            List<ItemStack> got = PENDING.remove(p.getUUID());
            if (got != null) for (ItemStack st : got) com.pvpciv.inv.Bag.give(p, st);
        }
        PENDING.clear();
        if (bar != null) bar.removeAllPlayers();
        clearMobs(l);
        if (demon != null && l.getEntity(demon) != null) l.getEntity(demon).discard();
        for (VoidDemon d : l.getEntitiesOfClass(VoidDemon.class, new AABB(C).inflate(60))) d.discard();
        clearArena(l);
        active = false;
        FIGHTERS.clear();
        demon = null;
    }

    // ------------------------------------------------------------------ блоки
    private static void disc(ServerLevel l, int y, BlockState s, BlockState ring) {
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
            double r = Math.hypot(x, z);
            if (r > R + 0.5) continue;
            l.setBlock(new BlockPos(C.getX() + x, y, C.getZ() + z), Math.abs(r - 6) < 0.5 || Math.abs(r - R) < 0.6 ? ring : s, 2);
        }
    }

    private static void clearLayer(ServerLevel l, int y) {
        for (int x = -26; x <= 26; x++) for (int z = -26; z <= 26; z++) {
            BlockPos q = new BlockPos(C.getX() + x, y, C.getZ() + z);
            if (!l.getBlockState(q).isAir()) l.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static void clearArena(ServerLevel l) {
        clearPath(l);
        for (int y = C.getY() - 3; y <= C.getY() + 4; y++) clearLayer(l, y);
    }

    private static void say(ServerLevel l, String msg) {
        for (ServerPlayer p : fighters(l.getServer())) p.sendSystemMessage(Component.literal("☠ Демоница: " + msg).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Смерть бойца разлома — обычная смерть; он выбывает (снимется в tick). */
    public static void onPlayerDeath(ServerPlayer p) { if (bar != null) bar.removePlayer(p); }
}
