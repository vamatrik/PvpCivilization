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
 * 1.4 — переделка: стадии по мане 10/40/25/25, всё в неоне.
 *  1. «Неоновые плиты» (100→90) — демоница называет цвет, чужие плиты гаснут и исчезают. Быстрый темп.
 *  2. «Лазеры» (90→50) — объёмные лучи с бегущей текстурой: низкие (прыжок), белые высокие (присесть/ползти),
 *     зелёные диагональные, вертикальные столбы (кольцо-предупреждение на полу), вращающийся луч. Сложность растёт плавно.
 *  3. «Стена» (50→25) — как в Stumble Guys: через арену едет стена. Перелезть по жёлтым ступеням или разбить бирюзовые
 *     блоки оружием и проползти (Z). Стена толкает, а не застревает в игроке; кто стоит на ней — едет вместе с ней.
 *  4. «Финал» (25→0) — стены выше и быстрее, плюс лазеры.
 * Мана кончилась — речь демоницы (приглашение в Ад, пропуск — Адский след), личные сундуки в центре,
 * через минуту всех вернёт на площадку (или раньше — шагни на телепорт в центре).
 */
public final class VoidRift {
    private VoidRift() {}
    public static final BlockPos C = new BlockPos(0, 150, 1600);
    private static final int R = 14, GRID = 10, TILE = 4;
    private static Block[] colors() { return new Block[]{ModRegistry.RIFT_NEON_RED.get(), ModRegistry.RIFT_NEON_BLUE.get(), ModRegistry.RIFT_NEON_YELLOW.get(), ModRegistry.RIFT_NEON_GREEN.get()}; }
    private static final int NCOL = 4;
    private static final String[] COLOR_NAMES = {"КРАСНЫЙ", "СИНИЙ", "ЖЁЛТЫЙ", "ЗЕЛЁНЫЙ"};
    private static final ChatFormatting[] COLOR_FMT = {ChatFormatting.RED, ChatFormatting.BLUE, ChatFormatting.YELLOW, ChatFormatting.GREEN};

    private static boolean active;
    private static int t, stage, sub, round, demonLevel = 34;   // 1.2: было 38 (мобы станции — до 30)
    private static float mana;
    private static UUID demon;
    private static final Set<UUID> FIGHTERS = new LinkedHashSet<>();
    private static ServerBossEvent bar;
    private static final int[][] grid = new int[GRID][GRID];
    private static int safeColor = -1;
    private static boolean tilesOff;
    private static final List<Beam> BEAMS = new ArrayList<>();
    private static final Map<UUID, Long> LASER_CD = new HashMap<>();
    private static final List<UUID> MOBS = new ArrayList<>();
    private static int endT = -1;
    private static long started, rageAt = -1;
    private static final Map<UUID, List<ItemStack>> PENDING = new HashMap<>();
    /** 1.2: выбывшие (умерли / ушли) — могут вернуться через телепорт у алтаря червя. */
    private static final Set<UUID> OUT = new LinkedHashSet<>();
    /** 1.2: все выбыли — ждём 2.5 мин, вдруг кто-то вернётся (тогда разлом с начала). */
    private static int waitT = -1;
    private static final int WAIT_MAX = 3000;
    private static Vec3 tpPoint() { return Vec3.atBottomCenterOf(com.pvpciv.floor.Belt.CENTER).add(0, 0, 3.5); }

    public static boolean active() { return active; }
    /** 1.0.1: музыка разлома — бойцам. */
    public static void music(MinecraftServer server, Map<UUID, String[]> out) {
        if (!active) return;
        long gt = server.overworld().getGameTime();
        if (rageAt < 0 && stage == 4 && endT < 0) rageAt = gt;   // 1.4: финал — рейдж-ремикс
        for (UUID u : FIGHTERS) out.put(u, com.pvpciv.floor.BossManager.musicAt("demon", started, rageAt, gt));
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
        active = true; t = 0; stage = 0; sub = 0; round = 0; mana = 100; endT = -1; waitT = -1; OUT.clear();
        started = s.overworld().getGameTime(); rageAt = -1;
        BEAMS.clear(); WALLS.clear(); trainLeft = 0; MOBS.clear(); CHESTS.clear(); CHEST_LOOT.clear();
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
        disc(l, C.getY() - 1, ModRegistry.RIFT_FLOOR.get().defaultBlockState(), ModRegistry.RIFT_FLOOR.get().defaultBlockState());   // цельный пол; кольца рисует клиент
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
        fx(l, 2);
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
            if (gone && p != null) { bar.removePlayer(p); PacketDistributor.sendToPlayer(p, new Network.PhoneData(31, "{\"clear\":true}")); PacketDistributor.sendToPlayer(p, new Network.PhoneData(31, "{\"fx\":0}")); }
            if (gone && endT < 0) {
                OUT.add(u);
                if (p != null) p.sendSystemMessage(Component.literal("☄ Ты выпал из разлома. Телепорт у алтаря червя (пояс астероидов) вернёт тебя в бой — успей, пока там кто-то держится" + (FIGHTERS.size() <= 1 ? " (или 2.5 мин, если не останется никого)." : ".")).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return gone;
        });
        if (endT < 0) returnTick(l);
        if (FIGHTERS.isEmpty()) {
            if (endT >= 0) { end(l); return; }   // 1.4: все ушли через телепорт финала
            if (OUT.isEmpty()) { fail(l); return; }
            if (waitT < 0) {   // 1.2: все выбыли — разлом замирает, 2.5 мин на возвращение
                waitT = 0;
                clearMobs(l); clearBeams(l); clearWall(l);
                if (demon != null && l.getEntity(demon) != null) l.getEntity(demon).discard();
                demon = null;
                for (UUID u : OUT) { ServerPlayer p = server.getPlayerList().getPlayer(u); if (p != null) p.sendSystemMessage(Component.literal("☠ В разломе никого. 2:30 — вернись через телепорт у алтаря червя, и разлом начнётся заново.").withStyle(ChatFormatting.DARK_PURPLE)); }
            }
            if (++waitT >= WAIT_MAX) { fail(l); return; }
            if (waitT % 600 == 0) for (UUID u : OUT) { ServerPlayer p = server.getPlayerList().getPlayer(u); if (p != null) p.displayClientMessage(Component.literal("Разлом закроется через " + (WAIT_MAX - waitT) / 20 + " с").withStyle(ChatFormatting.LIGHT_PURPLE), true); }
            return;
        }
        VoidDemon d = demon == null ? null : (l.getEntity(demon) instanceof VoidDemon vd ? vd : null);
        if (endT >= 0) { finishTick(l, d); return; }
        for (ServerPlayer p : fighters(server)) { fallCheck(l, p); if (stage >= 3) noFloatKick(p); }
        bar.setProgress(Math.max(0, mana / 100f));
        // стадии
        if (stage == 0) {
            if (t == 46) introPlatforms(l, d);
            if (t == 46 + 140) { removeIntroFloor(l); }
            if (t == 46 + 170) { stage = 1; setupStage(l, d); }
            return;
        }
        int want = mana > 90 ? 1 : mana > 50 ? 2 : mana > 25 ? 3 : 4;
        if (mana <= 0) { win(l, d); return; }
        if (want != stage) { stage = want; setupStage(l, d); }
        sub++;
        switch (stage) {
            case 1 -> { mana -= 10f / (36 * 20); colorFloor(l, d); }
            case 2 -> { mana -= 40f / (140 * 20); float lin = Math.max(0, Math.min(1, (90 - mana) / 40f)); lasers(l, d, lin, false); }
            case 3 -> { mana -= 25f / (80 * 20); walls(l, d, Math.max(0, Math.min(1, (50 - mana) / 25f)), false); }
            case 4 -> {
                mana -= 25f / (85 * 20);
                float lin = Math.max(0, Math.min(1, (25 - mana) / 25f));
                walls(l, d, lin, true);
                if (sub > 60) lasers(l, d, lin, true);
            }
            default -> {}
        }
        beamsTick(l);
        if (d != null) {
            Vec3 at = Vec3.atBottomCenterOf(C).add(Math.cos(t * 0.02) * 2, stage >= 3 ? 8 : 4 + Math.sin(t * 0.05), Math.sin(t * 0.02) * 2);
            d.teleportTo(at.x, at.y, at.z);
        }
    }

    private static void setupStage(ServerLevel l, VoidDemon d) {
        sub = 0;
        clearMobs(l);
        clearBeams(l);
        clearWall(l);
        fx(l, 0);
        trainLeft = 0;
        if (d != null) d.anim("cast");
        l.playSound(null, C, SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 3f, 1.4f);
        String[] names = {"", "НЕОНОВЫЕ ПЛИТЫ", "ЛАЗЕРЫ", "СТЕНА", "ФИНАЛ"};
        String[] hints = {"", "стой на плите названного цвета", "низкие — прыгай · белые — пригнись или ползи (Z)", "лезь по жёлтым ступеням или разбей бирюзовые блоки и ползи (Z)", "стены выше, лазеры не ждут"};
        for (ServerPlayer p : fighters(l.getServer())) {
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(names[stage]).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Стадия " + stage + "/4 · " + hints[stage]).withStyle(ChatFormatting.GRAY)));
        }
        SkySpin.start(l, Vec3.atCenterOf(C), 140, 80);   // 1.2: небо закручивается на смене стадии
        clearLayer(l, C.getY());
        switch (stage) {
            case 1 -> { round = 0; rollGrid(); drawGrid(l, -1, false); fx(l, 1); say(l, "«Любимые цвета? У меня — все. Но стоять можно только на одном.»"); }
            case 2 -> { floorDisc(l); fx(l, 2); say(l, "«Попрыгаем? Скакалку я принесла. Много скакалок.»"); }
            case 3 -> { floorDisc(l); fx(l, 2); say(l, "«Стенка едет. Перелезешь — молодец. Нет — полетаешь.»"); }
            case 4 -> { floorDisc(l); fx(l, 2); say(l, "«Последний танец. Не наступи мне на хвост!»"); }
            default -> {}
        }
    }

    /** 1.4: HD-оформление арены (рисует клиент): 0 — выкл, 1 — плиты, 2 — пол. */
    private static String fxJson(int mode) {
        if (mode == 0) return "{\"fx\":0}";
        int half = GRID * TILE / 2;
        if (mode == 1) return String.format(Locale.ROOT, "{\"fx\":1,\"at\":[%d,%d,%d],\"n\":%d,\"s\":%d}", C.getX() - half, C.getY() + 1, C.getZ() - half, GRID, TILE);
        return String.format(Locale.ROOT, "{\"fx\":2,\"at\":[%d,%d,%d],\"r\":%d}", C.getX(), stage == 0 && endT < 0 ? C.getY() : C.getY() + 1, C.getZ(), R);
    }
    private static void fx(ServerLevel l, int mode) {
        String js = fxJson(mode);
        for (ServerPlayer p : fighters(l.getServer())) PacketDistributor.sendToPlayer(p, new Network.PhoneData(31, js));
    }

    private static void floorDisc(ServerLevel l) {
        disc(l, C.getY(), ModRegistry.RIFT_FLOOR.get().defaultBlockState(), ModRegistry.RIFT_FLOOR.get().defaultBlockState());   // цельный пол; кольца рисует клиент
    }

    // ---- вступление: неоновые платформы над полом, затем пол исчезает
    private static void introPlatforms(ServerLevel l, VoidDemon d) {
        Block[] cs = colors();
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + Math.PI / 2 * k;
            int cx = C.getX() + (int) Math.round(Math.cos(a) * 7), cz = C.getZ() + (int) Math.round(Math.sin(a) * 7);
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) l.setBlock(new BlockPos(cx + x, C.getY(), cz + z), cs[k].defaultBlockState(), 2);
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
        fx(l, 0);
        l.playSound(null, C, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3f, 0.5f);
        l.sendParticles(ParticleTypes.REVERSE_PORTAL, C.getX() + 0.5, C.getY() - 1, C.getZ() + 0.5, 200, R / 2.0, 0.3, R / 2.0, 0.1);
    }

    // ---- стадия 1: неоновые плиты (быстрый темп; чужие плиты сначала гаснут — подсказка)
    private static void rollGrid() { Random r = new Random(); for (int[] row : grid) for (int j = 0; j < GRID; j++) row[j] = r.nextInt(NCOL); }

    private static void drawGrid(ServerLevel l, int onlyColor, boolean dimOthers) {
        int half = GRID * TILE / 2;
        Block[] cs = colors();
        BlockState off = ModRegistry.RIFT_NEON_OFF.get().defaultBlockState();
        for (int gx = 0; gx < GRID; gx++) for (int gz = 0; gz < GRID; gz++) {
            boolean on = onlyColor < 0 || grid[gx][gz] == onlyColor;
            BlockState s = on ? cs[grid[gx][gz]].defaultBlockState() : dimOthers ? off : Blocks.AIR.defaultBlockState();
            for (int x = 0; x < TILE; x++) for (int z = 0; z < TILE; z++)
                l.setBlock(new BlockPos(C.getX() - half + gx * TILE + x, C.getY(), C.getZ() - half + gz * TILE + z), s, 2);
        }
    }

    private static void colorFloor(ServerLevel l, VoidDemon d) {
        int announce = Math.max(22, 44 - round * 4), dim = 10, off = 28, rest = 12;
        int cyc = sub % (announce + off + rest);
        if (cyc == 0) {
            safeColor = new Random().nextInt(NCOL);
            for (ServerPlayer p : fighters(l.getServer())) {
                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(COLOR_NAMES[safeColor]).withStyle(COLOR_FMT[safeColor], ChatFormatting.BOLD)));
                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("встань на этот цвет").withStyle(ChatFormatting.GRAY)));
            }
            if (d != null) d.anim("point");
            l.playSound(null, C, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.HOSTILE, 3f, 0.6f + safeColor * 0.2f);
        } else if (cyc == announce - dim) {
            drawGrid(l, safeColor, true);   // чужие плиты гаснут
            l.playSound(null, C, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.HOSTILE, 2f, 0.7f);
        } else if (cyc == announce) {
            drawGrid(l, safeColor, false);
            tilesOff = true;
            l.playSound(null, C, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3f, 0.8f);
        } else if (cyc == announce + off) {
            rollGrid();
            drawGrid(l, -1, false);
            tilesOff = false;
            round++;
        }
    }

    // ---- лазеры: сервер считает попадания, клиент рисует (PhoneData 31) — луч шлётся один раз
    /** k: 0 — едущий отрезок, 1 — вращающийся, 2 — вертикальный столб. type: 0 низкий, 1 высокий. */
    private static final class Beam {
        int k, type, warn, life, color; Vec3 a, b = Vec3.ZERO, v = Vec3.ZERO; double len, ang, om, h, rv; boolean two; long t0; float w = 0.11f, dmg;
        Vec3[] ends(double tt) {
            return switch (k) {
                case 1 -> { double an = ang + om * tt; Vec3 dir = new Vec3(Math.cos(an), 0, Math.sin(an)).scale(len); yield new Vec3[]{two ? a.subtract(dir) : a, a.add(dir)}; }
                case 2 -> new Vec3[]{a, a.add(0, h, 0)};
                case 3 -> { Vec3 q = a.add(v.scale(tt)); yield new Vec3[]{q, q.add(0, h, 0)}; }
                case 4 -> { double an = ang + om * tt, r = Math.max(0.3, len + rv * tt); Vec3 q = a.add(Math.cos(an) * r, 0, Math.sin(an) * r); yield new Vec3[]{q, q.add(0, h, 0)}; }
                default -> new Vec3[]{a.add(v.scale(tt)), b.add(v.scale(tt))};
            };
        }
    }
    private static final int C_LOW = 0xFF30E0, C_HIGH = 0xE8E8FF, C_DIAG = 0x30FF60, C_COL = 0xFF5020, C_ROT = 0xFFA020;

    private static double floorTop() { return C.getY() + 1; }

    private static void send(ServerLevel l, Beam b) {
        BEAMS.add(b);
        StringBuilder s = new StringBuilder(String.format(Locale.ROOT, "{\"k\":%d,\"t0\":%d,\"warn\":%d,\"life\":%d,\"c\":%d,\"w\":%.3f,\"a\":[%.3f,%.3f,%.3f]",
                b.k, b.t0, b.warn, b.life, b.color, b.w, b.a.x, b.a.y, b.a.z));
        if (b.k == 0) s.append(String.format(Locale.ROOT, ",\"b\":[%.3f,%.3f,%.3f],\"v\":[%.4f,%.4f,%.4f]", b.b.x, b.b.y, b.b.z, b.v.x, b.v.y, b.v.z));
        if (b.k == 1) s.append(String.format(Locale.ROOT, ",\"len\":%.3f,\"ang\":%.4f,\"om\":%.5f,\"two\":%b", b.len, b.ang, b.om, b.two));
        if (b.k == 2) s.append(String.format(Locale.ROOT, ",\"h\":%.2f", b.h));
        if (b.k == 3) s.append(String.format(Locale.ROOT, ",\"h\":%.2f,\"v\":[%.4f,%.4f,%.4f]", b.h, b.v.x, b.v.y, b.v.z));
        if (b.k == 4) s.append(String.format(Locale.ROOT, ",\"h\":%.2f,\"len\":%.3f,\"ang\":%.4f,\"om\":%.5f,\"rv\":%.4f", b.h, b.len, b.ang, b.om, b.rv));
        s.append('}');
        for (ServerPlayer p : fighters(l.getServer())) PacketDistributor.sendToPlayer(p, new Network.PhoneData(31, s.toString()));
    }

    private static void clearBeams(ServerLevel l) {
        BEAMS.clear();
        for (ServerPlayer p : fighters(l.getServer())) PacketDistributor.sendToPlayer(p, new Network.PhoneData(31, "{\"clear\":true}"));
    }

    /** Прямой луч под углом deg (0 — вдоль X), едет по нормали через всю арену. */
    private static void straight(ServerLevel l, double deg, boolean high, int color, double speed, int warn, float dmg) {
        double an = Math.toRadians(deg);
        Vec3 dir = new Vec3(Math.cos(an), 0, Math.sin(an)), n = new Vec3(-dir.z, 0, dir.x);
        boolean fromNeg = new Random().nextBoolean();
        double y = floorTop() + (high ? 1.62 : 0.3);
        Vec3 o = new Vec3(C.getX() + 0.5, y, C.getZ() + 0.5).add(n.scale(fromNeg ? -(R + 1.5) : R + 1.5));
        Beam b = new Beam();
        b.k = 0; b.type = high ? 1 : 0; b.color = color; b.w = high ? 0.1f : 0.11f; b.dmg = dmg;
        b.a = o.subtract(dir.scale(R + 2)); b.b = o.add(dir.scale(R + 2));
        b.v = n.scale(fromNeg ? speed : -speed);
        b.warn = warn; b.t0 = l.getGameTime() + warn; b.life = (int) Math.ceil((2 * R + 3) / speed);
        send(l, b);
    }

    private static void column(ServerLevel l, double x, double z, int warn, float dmg) {
        Beam b = new Beam();
        b.k = 2; b.type = 2; b.color = C_COL; b.w = 0.42f; b.dmg = dmg; b.h = 7;
        b.a = new Vec3(x, floorTop(), z);
        b.warn = warn; b.t0 = l.getGameTime() + warn; b.life = 14;
        send(l, b);
    }

    private static void sweeper(ServerLevel l, double om, int warn, float dmg) {
        Beam b = new Beam();
        b.k = 1; b.type = 0; b.color = C_ROT; b.w = 0.12f; b.dmg = dmg; b.two = true; b.len = R + 1;
        b.a = new Vec3(C.getX() + 0.5, floorTop() + 0.3, C.getZ() + 0.5);
        b.ang = new Random().nextDouble() * Math.PI; b.om = om * (new Random().nextBoolean() ? 1 : -1);
        b.warn = warn; b.t0 = l.getGameTime() + warn; b.life = 200;
        send(l, b);
    }

    /** 1.5.3: едущий вертикальный луч: off — смещение поперёк хода (вдоль dir), едет по нормали через арену. */
    private static void vbar(ServerLevel l, double deg, double off, boolean fromNeg, double speed, int warn, float dmg, int color) {
        double an = Math.toRadians(deg);
        Vec3 dir = new Vec3(Math.cos(an), 0, Math.sin(an)), n = new Vec3(-dir.z, 0, dir.x);
        Beam b = new Beam();
        b.k = 3; b.type = 2; b.color = color; b.w = 0.24f; b.dmg = dmg; b.h = 4.2;
        b.a = new Vec3(C.getX() + 0.5, floorTop(), C.getZ() + 0.5).add(n.scale(fromNeg ? -(R + 1.5) : R + 1.5)).add(dir.scale(off));
        b.v = n.scale(fromNeg ? speed : -speed);
        b.warn = warn; b.t0 = l.getGameTime() + warn; b.life = (int) Math.ceil((2 * R + 3) / speed);
        send(l, b);
    }

    /** Ряд вертикальных лучей с проёмом — найди дыру. */
    private static void vrow(ServerLevel l, double speed, int warn, float dmg) {
        Random r = new Random();
        double deg = r.nextBoolean() ? 0 : 90;
        boolean fromNeg = r.nextBoolean();
        double step = 1.15;
        int n = (int) ((2 * R + 2) / step);
        int gap = 3 + r.nextInt(Math.max(1, n - 8)), gapW = 3;
        for (int i = 0; i < n; i++) {
            if (i >= gap && i < gap + gapW) continue;
            vbar(l, deg, -(R + 1) + i * step, fromNeg, speed, warn, dmg, 0xFF4060);
        }
    }

    /** Кружащая группа вертикальных лучей: n штук на радиусе r0, вращение om, радиус меняется на rv за тик. */
    private static void orbit(ServerLevel l, int n, double r0, double om, double rv, int life, int warn, float dmg) {
        double a0 = new Random().nextDouble() * Math.PI * 2;
        for (int i = 0; i < n; i++) {
            Beam b = new Beam();
            b.k = 4; b.type = 2; b.color = 0xFF7030; b.w = 0.24f; b.dmg = dmg; b.h = 4.2;
            b.a = new Vec3(C.getX() + 0.5, floorTop(), C.getZ() + 0.5);
            b.len = r0; b.ang = a0 + Math.PI * 2 * i / n; b.om = om; b.rv = rv;
            b.warn = warn; b.t0 = l.getGameTime() + warn; b.life = life;
            send(l, b);
        }
    }

    /**
     * lin 0..1 — прогресс стадии; сложность растёт плавно. 1.5.3: едущие вертикальные лучи (по одному, рядами с проёмом,
     * кружащими группами), плотнее к концу. В финале (последние 25%) — тяжело: лучи поверх стен.
     */
    private static void lasers(ServerLevel l, VoidDemon d, float lin, boolean finale) {
        float f = finale ? 0.75f + lin * 0.25f : lin;
        int maxN = 2 + Math.round(f * 5) + (finale ? 1 : 0);
        int every = finale ? 34 - (int) (lin * 12) : 64 - (int) (f * 40);
        int warn = Math.max(14, 26 - (int) (f * 10));
        double speed = 0.15 + f * 0.22;
        float dmg = 0.16f + f * 0.1f;
        Random rnd = new Random();
        if (sub % Math.max(16, every) != 0) return;
        int moving = 0; boolean orbiting = false, sweeping = false;
        for (Beam b : BEAMS) { if (b.k == 0 || b.k == 3) moving++; if (b.k == 4) orbiting = true; if (b.k == 1) sweeping = true; }
        float roll = rnd.nextFloat();
        // кружащая группа
        if (f > 0.4f && !orbiting && roll < (finale ? 0.3f : 0.22f)) {
            int n = 3 + Math.round(f * 4);
            double r0 = 4 + rnd.nextDouble() * 7, rv = rnd.nextFloat() < 0.5f ? 0 : (r0 > 8 ? -0.025 : 0.025);
            orbit(l, n, r0, (0.018 + f * 0.022) * (rnd.nextBoolean() ? 1 : -1), rv, 160 + Math.round(f * 80), warn + 10, dmg + 0.04f);
            if (d != null) d.anim("cast");
            l.playSound(null, C, SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.5f, 0.8f);
            return;
        }
        // ряд с проёмом
        if (f > 0.55f && moving < maxN && roll < (finale ? 0.5f : 0.36f)) {
            vrow(l, speed * 0.75, warn + 8, dmg + 0.04f);
            if (d != null) d.anim("point");
            l.playSound(null, C, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.HOSTILE, 1.2f, 0.7f);
            return;
        }
        // вращающийся низкий луч
        if (f > 0.65f && !sweeping && roll < (finale ? 0.62f : 0.46f)) {
            sweeper(l, 0.028 + f * 0.014, warn + 10, dmg);
            return;
        }
        if (moving >= maxN) return;
        int burst = f > 0.6f && rnd.nextFloat() < 0.35f + f * 0.25f ? 2 : 1;
        if (finale && rnd.nextFloat() < 0.35f) burst = 3;
        double baseDeg = rnd.nextBoolean() ? 0 : 90;
        for (int k = 0; k < burst; k++) {
            float t = rnd.nextFloat();
            if (f > 0.2f && t < 0.2f + f * 0.15f) {   // одиночный едущий вертикальный
                vbar(l, rnd.nextBoolean() ? 0 : 90, (rnd.nextDouble() * 2 - 1) * (R - 3), rnd.nextBoolean(), speed * 0.85, warn, dmg + 0.03f, 0xFF5020);
                continue;
            }
            boolean diag = f > 0.25f && rnd.nextFloat() < 0.35f;
            boolean high = !diag && f > 0.15f && rnd.nextFloat() < 0.2f + f * 0.2f;
            double deg = diag ? (rnd.nextBoolean() ? 45 : 135) : (burst >= 2 ? baseDeg + 90 * k : baseDeg);
            straight(l, deg, high, diag ? C_DIAG : high ? C_HIGH : C_LOW, speed * (diag ? 0.9 : 1), warn, dmg);
        }
        if (d != null) d.anim("point");
        l.playSound(null, C, SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.HOSTILE, 1.0f, 1.6f);   // тихо: «щёлк» — луч на подходе
    }

    private static void beamsTick(ServerLevel l) {
        if (BEAMS.isEmpty()) return;
        long gt = l.getGameTime();
        BEAMS.removeIf(b -> gt > b.t0 + b.life);
        for (Beam b : BEAMS) {
            double tt = gt - b.t0;
            if (tt < 0 || tt > b.life) continue;
            for (ServerPlayer p : fighters(l.getServer())) {
                if (p.getY() < C.getY()) continue;
                if (!touches(b, tt, p) && !touches(b, tt - 0.5, p)) continue;
                long now = gt;
                if (LASER_CD.getOrDefault(p.getUUID(), 0L) > now) continue;
                LASER_CD.put(p.getUUID(), now + 12);
                p.invulnerableTime = 0;
                p.hurt(l.damageSources().magic(), p.getMaxHealth() * b.dmg);
                p.setDeltaMovement(p.getDeltaMovement().add(0, b.type == 1 ? -0.2 : 0.45, 0));
                p.hurtMarked = true;
                l.sendParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 0.5, p.getZ(), 1, 0, 0, 0, 0);
                l.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.8f, 1.6f);
            }
        }
    }

    private static boolean touches(Beam b, double tt, ServerPlayer p) {
        if (tt < 0) return false;
        AABB bb = p.getBoundingBox();
        Vec3[] e = b.ends(tt);
        if (b.k >= 2) return Math.hypot(p.getX() - e[0].x, p.getZ() - e[0].z) < b.w + 0.33 && bb.minY < e[1].y && bb.maxY > e[0].y;
        double y = e[0].y;
        if (!(bb.minY < y + b.w && bb.maxY > y - b.w)) return false;
        double ax = e[0].x, az = e[0].z, dx = e[1].x - ax, dz = e[1].z - az, px = p.getX() - ax, pz = p.getZ() - az;
        double L2 = dx * dx + dz * dz, k = L2 < 1e-6 ? 0 : Math.max(0, Math.min(1, (px * dx + pz * dz) / L2));
        return Math.hypot(px - dx * k, pz - dz * k) < b.w + 0.32;
    }

    // ---- стадии 3–4: едущие стены (1.5.3 — плавные, из движущихся столбиков-сущностей, поездом без пауз)
    /** Одна стена поезда: ось (0 — едет по X), направление, скорость, где сейчас (вдоль оси, от центра), её столбики. */
    private static final class Wall {
        int axis, s, id, layers; double along, v; final List<UUID> parts = new ArrayList<>();
        Vec3 make(double al, double lat, double y) { return axis == 0 ? new Vec3(C.getX() + 0.5 + al, y, C.getZ() + 0.5 + lat) : new Vec3(C.getX() + 0.5 + lat, y, C.getZ() + 0.5 + al); }
        boolean out() { return along * s > R + 3 + layers; }
    }
    private static final List<Wall> WALLS = new ArrayList<>();
    private static int trainLeft, trainAxis, trainS, wallSeq;
    private static Wall trainLast;
    private enum Pat { LADDER, HOLES, LIMBO, HURDLE, GAP, MIX }

    private static void walls(ServerLevel l, VoidDemon d, float lin, boolean finale) {
        for (var it = WALLS.iterator(); it.hasNext(); ) {
            Wall w = it.next();
            w.along += w.v * w.s;
            if (w.out()) { removeWall(l, w); it.remove(); }
        }
        Wall last = WALLS.isEmpty() ? null : WALLS.get(WALLS.size() - 1);
        double spacing = finale ? 9.5 : 11.5;
        if (trainLeft > 0) {
            // следующая стена поезда — как только предыдущая отъехала на spacing
            if (trainLast == null || (trainLast.along * trainLast.s + R + 2) >= spacing) { spawnWall(l, lin, finale); trainLast = WALLS.get(WALLS.size() - 1); trainLeft--; }
            return;
        }
        // новый поезд — с перпендикулярной стороны, когда хвост прошлого уже за центром (стены не пересекаются в круге)
        if (last == null || last.along * last.s > 8.5) {
            trainAxis = last == null ? new Random().nextInt(2) : 1 - last.axis;
            trainS = new Random().nextBoolean() ? 1 : -1;
            trainLeft = finale ? 5 + new Random().nextInt(3) : 4 + new Random().nextInt(2);
            trainLast = null;
            if (d != null) d.anim("cast");
            l.playSound(null, C, SoundEvents.PISTON_EXTEND, SoundSource.HOSTILE, 3f, 0.5f);
        }
    }

    private static void spawnWall(ServerLevel l, float lin, boolean finale) {
        Random r = new Random();
        Wall w = new Wall();
        w.axis = trainAxis; w.s = trainS; w.id = ++wallSeq;
        w.v = finale ? 0.17 + lin * 0.06 : 0.12 + lin * 0.05;
        int H = finale && r.nextFloat() < 0.6f ? 4 : 3;
        Pat[] pool = finale ? new Pat[]{Pat.LADDER, Pat.HOLES, Pat.LIMBO, Pat.HURDLE, Pat.GAP, Pat.MIX, Pat.LIMBO, Pat.MIX}
                : lin < 0.35f ? new Pat[]{Pat.LADDER, Pat.HOLES, Pat.HURDLE, Pat.GAP} : new Pat[]{Pat.LADDER, Pat.HOLES, Pat.LIMBO, Pat.HURDLE, Pat.GAP, Pat.MIX};
        Pat pat = pool[r.nextInt(pool.length)];
        int N = 2 * R + 3, OFF = R + 1;
        int[][] cell = new int[N][H];   // 0 пусто, 1 стена, 2 хрупкий
        boolean[] ladder = new boolean[N];
        for (int[] c : cell) Arrays.fill(c, 1);
        List<Integer> used = new ArrayList<>();
        switch (pat) {
            case LADDER -> { for (int i = 0; i < (finale ? 1 : 2); i++) { int lc = pick(r, used, R - 3); for (int k = 0; k < 2; k++) { ladder[lc + k + OFF] = true; cell[lc + k + OFF][H - 1] = 0; } } }
            case HOLES -> { for (int i = 0; i < (finale ? 2 : 3); i++) { int li = pick(r, used, R - 2) + OFF; cell[li][0] = 2; if (r.nextFloat() < (finale ? 0.2f : 0.4f)) cell[li][1] = 2; } }
            case LIMBO -> { for (int li = 0; li < N; li++) if ((li % 7) != 3) cell[li][0] = 0; }   // нижний ряд открыт — под стену только ползком (Z)
            case HURDLE -> { H = 1; cell = new int[N][1]; for (int[] c : cell) c[0] = 1; }   // низкий барьер — перепрыгни
            case GAP -> { int g = pick(r, used, R - 4) + OFF; for (int k = 0; k < (finale ? 1 : 2); k++) for (int h = 0; h < H; h++) cell[g + k][h] = 0; }
            case MIX -> {
                int lc = pick(r, used, R - 3); for (int k = 0; k < 2; k++) { ladder[lc + k + OFF] = true; cell[lc + k + OFF][H - 1] = 0; }
                int li = pick(r, used, R - 2) + OFF; cell[li][0] = 2;
            }
        }
        w.layers = 0;
        for (boolean b : ladder) if (b) w.layers = Math.max(w.layers, H - 2);
        w.along = -w.s * (R + 2.0);
        double vx = w.axis == 0 ? w.v * w.s : 0, vz = w.axis == 0 ? 0 : w.v * w.s;
        for (int li = 0; li < N; li++) {
            int lat = li - OFF;
            if (Math.abs(lat) > R + 0.5) continue;
            // столбики подряд одного вида
            int h = 0;
            while (h < H) {
                int k = cell[li][h];
                if (k == 0) { h++; continue; }
                int h1 = h;
                if (k == 1) while (h1 + 1 < H && cell[li][h1 + 1] == 1) h1++;
                part(l, w, w.along, lat, h, h1 - h + 1, k == 2 ? 1 : 0, vx, vz);
                h = h1 + 1;
            }
            if (ladder[li]) for (int k = 1; k <= H - 2; k++) part(l, w, w.along + w.s * k, lat, 0, H - 1 - k, 2, vx, vz);
        }
        WALLS.add(w);
        String hint = switch (pat) {
            case LADDER -> "▮ Стена: жёлтые ступени — перелезь";
            case HOLES -> "▮ Стена: разбей бирюзовые блоки и пролезь (Z — ползти)";
            case LIMBO -> "▮ Низкая щель — ползи под стеной (Z)";
            case HURDLE -> "▮ Барьер — прыгай!";
            case GAP -> "▮ Проём в стене — успей в него";
            case MIX -> "▮ Стена: ступени или хрупкий блок";
        };
        for (ServerPlayer p : fighters(l.getServer())) p.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(hint).withStyle(ChatFormatting.LIGHT_PURPLE)));
    }

    private static void part(ServerLevel l, Wall w, double along, int lat, int h, int len, int kind, double vx, double vz) {
        var e = ModRegistry.RIFT_WALL_E.get().create(l);
        if (e == null) return;
        Vec3 at = w.make(along, lat, floorTop() + h);
        e.moveTo(at.x, at.y, at.z, 0, 0);
        e.setup(kind, len, vx, vz, w.id);
        l.addFreshEntity(e);
        w.parts.add(e.getUUID());
    }

    private static int pick(Random r, List<Integer> used, int span) {
        for (int tries = 0; tries < 40; tries++) {
            int c = r.nextInt(2 * span + 1) - span;
            boolean ok = true;
            for (int u : used) if (Math.abs(u - c) < 4) { ok = false; break; }
            if (ok || tries == 39) { used.add(c); return c; }
        }
        return 0;
    }

    private static void removeWall(ServerLevel l, Wall w) {
        for (UUID u : w.parts) { var e = l.getEntity(u); if (e != null) e.discard(); }
        w.parts.clear();
    }

    private static void clearWall(ServerLevel l) {
        for (Wall w : WALLS) removeWall(l, w);
        WALLS.clear();
        trainLeft = 0;
        for (var e : l.getEntitiesOfClass(com.pvpciv.entity.RiftWallEntity.class, new AABB(C).inflate(40))) e.discard();
    }

    /** Стоять на едущей стене (сущность, не блок) сервер считает «парением» — сбрасываем счётчик, чтобы не кикнуло. */
    private static java.lang.reflect.Field FLOAT;
    private static void noFloatKick(ServerPlayer p) {
        try {
            if (FLOAT == null) { FLOAT = net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("aboveGroundTickCount"); FLOAT.setAccessible(true); }
            FLOAT.setInt(p.connection, 0);
        } catch (Exception ignored) {}
    }

    /** Безопасная точка на оси поезда — не в стене (для возврата после падения). */
    private static Vec3 safeSpot() {
        if (WALLS.isEmpty()) return Vec3.atBottomCenterOf(C).add(0, 1, 0);
        Wall ref = WALLS.get(WALLS.size() - 1);
        for (double a = 0; a <= R - 3; a += 1) for (int sg : new int[]{1, -1}) {
            double al = a * sg;
            boolean ok = true;
            for (Wall w : WALLS) if (w.axis == ref.axis && Math.abs(w.along - al) < 2.5 + (w.layers + 1)) { ok = false; break; }
            if (ok) return ref.make(al, 0, floorTop());
        }
        return Vec3.atBottomCenterOf(C).add(0, 1, 0);
    }

    /** Старые блочные стены (до 1.5.3) — больше не используются. */
    public static boolean tryBreak(ServerPlayer p, BlockPos q) { return false; }
    public static void onSwing(ServerPlayer p) {}

    private static void clearMobs(ServerLevel l) {
        for (UUID u : MOBS) { var e = l.getEntity(u); if (e != null) { l.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 1, e.getZ(), 10, 0.3, 0.5, 0.3, 0.02); e.discard(); } }
        MOBS.clear();
    }

    /** Гравитация разлома снята в 1.4 (паркур ушёл) — только убираем старый модификатор, если остался. */
    private static final ResourceLocation RIFT_GRAV = com.pvpciv.PvpCiv.id("rift_gravity");
    private static void gravSync(MinecraftServer server) {
        if (server.getTickCount() % 40 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            var g = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY);
            if (g != null && g.getModifier(RIFT_GRAV) != null) g.removeModifier(RIFT_GRAV);
        }
    }

    // ------------------------------------------------------------------ падения
    private static void fallCheck(ServerLevel l, ServerPlayer p) {
        if (p.getY() > C.getY() - 12) return;
        p.resetFallDistance();
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
        if (!WALLS.isEmpty()) to = safeSpot();   // не в стену
        p.teleportTo(to.x, to.y, to.z);
        p.displayClientMessage(Component.literal("Пустота выплюнула тебя обратно: −30% здоровья").withStyle(ChatFormatting.RED), true);
    }

    public static void onDemonHit(ServerPlayer p, float amount) {
        if (!active || stage == 0 || endT >= 0) return;
        mana -= 0.12f;
        p.serverLevel().sendParticles(ParticleTypes.WITCH, p.getX(), p.getY() + 1, p.getZ(), 4, 0.3, 0.3, 0.3, 0);
    }

    // ------------------------------------------------------------------ конец: речь, личные сундуки, телепорт
    /** Личные сундуки: владелец → позиция; маркеры-имена над ними. */
    private static final Map<UUID, BlockPos> CHESTS = new LinkedHashMap<>();
    private static final List<UUID> MARKERS = new ArrayList<>();
    private static final Map<UUID, List<ItemStack>> CHEST_LOOT = new HashMap<>();
    private static final int CHEST_AT = 300, LEAVE_T = CHEST_AT + 1200;

    private static void win(ServerLevel l, VoidDemon d) {
        endT = 0;
        fx(l, 0);
        clearMobs(l);
        clearBeams(l);
        clearWall(l);
        bar.setProgress(0);
        for (int y = C.getY() + 1; y <= C.getY() + 6; y++) clearLayer(l, y);
        clearLayer(l, C.getY());
        floorDisc(l);
        fx(l, 2);
        if (d != null) { d.anim("laugh"); d.teleportTo(C.getX() + 0.5, C.getY() + 4, C.getZ() + 0.5); }
        l.playSound(null, C, SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 3f, 1.4f);
        List<ServerPlayer> fs = fighters(l.getServer());
        int i = 0;
        for (ServerPlayer p : fs) {
            double a = Math.PI * 2 * i++ / Math.max(1, fs.size());
            double x = C.getX() + 0.5 + Math.cos(a) * 7, z = C.getZ() + 0.5 + Math.sin(a) * 7;
            float yaw = (float) Math.toDegrees(Math.atan2(C.getZ() + 0.5 - z, C.getX() + 0.5 - x)) - 90;
            p.teleportTo(l, x, floorTop(), z, yaw, -15);
            p.setDeltaMovement(Vec3.ZERO);
            p.resetFallDistance();
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("МАНА ИССЯКЛА").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Демоница хочет что-то сказать…").withStyle(ChatFormatting.GRAY)));
        }
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, C.getX() + 0.5 + Math.cos(a) * 10, C.getY() + 1, C.getZ() + 0.5 + Math.sin(a) * 10, 1, 0, 0, 0, 0);
        }
        l.playSound(null, C, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3f, 0.6f);
        for (ServerPlayer o : l.getServer().getPlayerList().getPlayers())
            o.sendSystemMessage(Component.literal("☠ Демоница Пустоты истратила всю ману! Галактический червь пал. Первый мир почти пройден…").withStyle(ChatFormatting.DARK_PURPLE));
    }

    /** Речь демоницы: [тик, реплика]. */
    private static final Object[][] SPEECH = {
            {30, "«Ах… мана на нуле. Давно меня так не выжимали.»"},
            {95, "«Знаешь, ты мне нравишься. Червячка не жалко — он был скучный.»"},
            {160, "«Внизу, под станцией, — мой дом. Ад. Там тепло, весело… и очень-очень больно.»"},
            {225, "«Держи пропуск — Адский след. Почини лифт станции, и он довезёт тебя ко мне.»"},
            {CHEST_AT - 10, "«А это — чтобы ты не передумал. Не скучай без меня!»"}};

    private static void finishTick(ServerLevel l, VoidDemon d) {
        endT++;
        List<ServerPlayer> fs = fighters(l.getServer());
        for (Object[] s : SPEECH) if (endT == (int) s[0]) {
            say(l, (String) s[1]);
            for (ServerPlayer p : fs) p.connection.send(new ClientboundSetActionBarTextPacket(Component.literal((String) s[1]).withStyle(ChatFormatting.LIGHT_PURPLE)));
            if (d != null) d.anim(endT < 200 ? "laugh" : "point");
            l.playSound(null, C, SoundEvents.ALLAY_AMBIENT_WITH_ITEM, SoundSource.HOSTILE, 2f, 0.7f);
        }
        if (d != null && endT < CHEST_AT) {
            d.teleportTo(C.getX() + 0.5 + Math.cos(endT * 0.03) * 0.6, C.getY() + 4 + Math.sin(endT * 0.07) * 0.3, C.getZ() + 0.5 + Math.sin(endT * 0.03) * 0.6);
            if (endT % 5 == 0) l.sendParticles(ParticleTypes.FLAME, d.getX(), d.getY() + 1, d.getZ(), 4, 0.4, 0.8, 0.4, 0.01);
        }
        if (endT == 225) for (ServerPlayer p : fs) {   // пропуск в Ад — сразу в сумку
            com.pvpciv.inv.Bag.give(p, new ItemStack(ModF5.HELL_TRAIL.get()));
            if (d != null) {
                Vec3 a = d.position().add(0, 1.2, 0), b = p.position().add(0, 1, 0);
                for (int k = 0; k <= 16; k++) { Vec3 q = a.lerp(b, k / 16.0); l.sendParticles(ParticleTypes.FLAME, q.x, q.y, q.z, 2, 0.05, 0.05, 0.05, 0); }
            }
            p.sendSystemMessage(Component.literal("Получен пропуск: Адский след (лифт станции → Ад)").withStyle(ChatFormatting.GOLD));
            l.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1f, 0.8f);
        }
        if (endT == CHEST_AT) {
            spawnChests(l, fs);
            if (d != null) {
                l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, d.getX(), d.getY() + 1, d.getZ(), 1, 0, 0, 0, 0);
                l.sendParticles(ParticleTypes.LAVA, d.getX(), d.getY() + 1, d.getZ(), 30, 0.6, 1, 0.6, 0);
                l.playSound(null, d.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.HOSTILE, 1f, 1.5f);
                d.discard(); demon = null;
            }
            for (ServerPlayer p : fs) {
                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("ИЗГНАНА!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Сундук и телепорт домой — в центре").withStyle(ChatFormatting.GRAY)));
            }
        }
        if (endT > CHEST_AT) {
            int left = (LEAVE_T - endT) / 20;
            if (endT % 20 == 0) {
                for (ServerPlayer p : fs) p.connection.send(new ClientboundSetActionBarTextPacket(Component.literal("Возврат на площадку через " + left + " с · или встань на телепорт в центре").withStyle(ChatFormatting.LIGHT_PURPLE)));
                l.sendParticles(ParticleTypes.REVERSE_PORTAL, C.getX() + 0.5, floorTop() + 0.2, C.getZ() + 0.5, 20, 0.5, 0.1, 0.5, 0.05);
            }
            if (endT % 4 == 0) l.sendParticles(ParticleTypes.WITCH, C.getX() + 0.5, floorTop() + 0.1, C.getZ() + 0.5, 3, 0.4, 0.05, 0.4, 0);
            for (ServerPlayer p : fs)
                if (Math.abs(p.getX() - C.getX() - 0.5) < 0.9 && Math.abs(p.getZ() - C.getZ() - 0.5) < 0.9 && Math.abs(p.getY() - floorTop()) < 0.6) leave(l, p);
            if (endT >= LEAVE_T) end(l);
        }
    }

    private static void spawnChests(ServerLevel l, List<ServerPlayer> fs) {
        l.setBlock(new BlockPos(C.getX(), C.getY(), C.getZ()), ModRegistry.RIFT_PAD.get().defaultBlockState(), 2);
        int n = Math.max(1, fs.size()), i = 0;
        for (ServerPlayer p : fs) {
            double a = Math.PI * 2 * i++ / n + Math.PI / 4;
            int rad = n <= 4 ? 3 : 4;
            BlockPos q = new BlockPos(C.getX() + (int) Math.round(Math.cos(a) * rad), C.getY() + 1, C.getZ() + (int) Math.round(Math.sin(a) * rad));
            net.minecraft.core.Direction face = net.minecraft.core.Direction.fromYRot(Math.toDegrees(Math.atan2(q.getZ() - C.getZ(), q.getX() - C.getX())) - 90);
            l.setBlock(q, Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, face), 3);
            CHEST_LOOT.put(p.getUUID(), rollLoot(l, p));
            CHESTS.put(p.getUUID(), q);
            net.minecraft.world.entity.decoration.ArmorStand m = new net.minecraft.world.entity.decoration.ArmorStand(l, q.getX() + 0.5, q.getY() + 0.65, q.getZ() + 0.5);
            m.setInvisible(true); m.setNoGravity(true); m.setInvulnerable(true); m.setSilent(true);
            m.setCustomName(Component.literal("★ " + p.getGameProfile().getName()).withStyle(ChatFormatting.GOLD));
            m.setCustomNameVisible(true);
            CompoundHack.marker(m);
            l.addFreshEntity(m);
            MARKERS.add(m.getUUID());
            l.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, q.getX() + 0.5, q.getY() + 1, q.getZ() + 0.5, 40, 0.4, 0.5, 0.4, 0.3);
        }
        l.playSound(null, C, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.5f, 1f);
    }

    /** Маркер-стойка без хитбокса (через NBT — сеттер приватный). */
    private static final class CompoundHack {
        static void marker(net.minecraft.world.entity.decoration.ArmorStand m) {
            net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
            m.addAdditionalSaveData(t);
            t.putBoolean("Marker", true);
            t.putBoolean("Invisible", true);
            m.readAdditionalSaveData(t);
        }
    }

    private static List<ItemStack> rollLoot(ServerLevel l, ServerPlayer p) {
        StoryData sd = Progress.d(p);
        boolean first = (sd.trophies & 512) == 0;
        List<ItemStack> loot = new ArrayList<>();
        LootTables.PREFER = Classes.prefer(p);
        try { loot.addAll(LootTables.roll("floor5_final_personal", l.random, 1.0)); } finally { LootTables.PREFER = null; }
        loot.addAll(LootTables.roll("floor5_final_ground", l.random, 1.0 + 0.15 * (FIGHTERS.size() - 1)));
        if (first) {
            sd.trophies |= 512;
            p.setData(ModRegistry.STORY.get(), sd);
            loot.add(new ItemStack(ModF5.DARK_STAR.get()));
            loot.add(new ItemStack(com.pvpciv.registry.ModV11.DEMON_TROPHY.get()));
            Classes.giveMatter(p, 2);
        }
        for (ItemStack st : loot) Story.announceRareDrop(l, st, p.getGameProfile().getName());
        Progress.addXp(p, Progress.xpFor(sd.level, demonLevel, 20));
        Progress.addMoney(p, 20000, true);
        com.pvpciv.progress.Titles.grant(p, "hell_walker");
        
        return loot;
    }

    /** Забрать остаток из сундука игрока (в сумку) и убрать сундук. */
    private static void drainChest(ServerLevel l, UUID u) {
        BlockPos q = CHESTS.remove(u);
        if (q == null) return;
        ServerPlayer p = l.getServer().getPlayerList().getPlayer(u);
        List<ItemStack> got = CHEST_LOOT.remove(u);
        if (got != null && p != null) for (ItemStack st : got) com.pvpciv.inv.Bag.give(p, st);
        l.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
        l.sendParticles(ParticleTypes.REVERSE_PORTAL, q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.05);
        for (var e : l.getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class, new AABB(q).inflate(0.6, 1.5, 0.6))) if (MARKERS.remove(e.getUUID())) e.discard();
    }

    /** Одноразовый телепорт финала: шагнул — забрал остаток сундука и вернулся на площадку. */
    private static void leave(ServerLevel l, ServerPlayer p) {
        drainChest(l, p.getUUID());
        FIGHTERS.remove(p.getUUID());
        bar.removePlayer(p);
        BlockPos pad = com.pvpciv.floor.Belt.PAD;
        p.teleportTo(l, pad.getX() + 0.5, pad.getY(), pad.getZ() + 0.5, 180, 0);
        p.resetFallDistance();
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(31, "{\"clear\":true}"));
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(31, "{\"fx\":0}"));
        l.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5f, 1.6f);
    }

    /** ПКМ по сундуку демоницы: свой — награда в сумку, чужой — нельзя. */
    public static boolean blockedChest(ServerPlayer p, BlockPos q) {
        if (CHESTS.isEmpty()) return false;
        for (var en : new ArrayList<>(CHESTS.entrySet())) if (en.getValue().equals(q)) {
            if (!en.getKey().equals(p.getUUID())) {
                p.displayClientMessage(Component.literal("Это не твой сундук — твой подписан твоим именем.").withStyle(ChatFormatting.RED), true);
                return true;
            }
            ServerLevel l = p.serverLevel();
            l.blockEvent(q, Blocks.CHEST, 1, 1);
            l.playSound(null, q, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1f, 0.9f);
            l.playSound(null, q, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.3f);
            l.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, q.getX() + 0.5, q.getY() + 1, q.getZ() + 0.5, 30, 0.3, 0.4, 0.3, 0.25);
            List<ItemStack> got = CHEST_LOOT.get(p.getUUID());
            if (got != null) p.sendSystemMessage(Component.literal("Из сундука в сумку: " + Story.lootLine(got)).withStyle(ChatFormatting.GOLD));
            drainChest(l, p.getUUID());
            return true;
        }
        return false;
    }

    /** 1.2: телепорт у алтаря червя — выбывшие возвращаются (если все выбыли — разлом начинается заново). */
    private static void returnTick(ServerLevel l) {
        if (OUT.isEmpty()) return;
        Vec3 tp = tpPoint();
        if (t % 4 == 0) {
            l.sendParticles(ParticleTypes.REVERSE_PORTAL, tp.x, tp.y + 1, tp.z, 12, 0.4, 1.0, 0.4, 0.05);
            l.sendParticles(ParticleTypes.WITCH, tp.x, tp.y + 0.1, tp.z, 4, 0.6, 0.05, 0.6, 0);
        }
        for (var it = OUT.iterator(); it.hasNext(); ) {
            ServerPlayer p = l.getServer().getPlayerList().getPlayer(it.next());
            if (p == null || !p.isAlive() || p.level() != l || p.position().distanceTo(tp) > 1.8) continue;
            it.remove();
            FIGHTERS.add(p.getUUID());
            bar.addPlayer(p);
            l.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.6f, 1.4f);
            if (waitT >= 0) {   // все выбыли — с начала
                waitT = -1;
                stage = 0; sub = 0; round = 0; mana = 100; t = 44; rageAt = -1;
                BEAMS.clear(); WALLS.clear(); trainLeft = 0;
                PacketDistributor.sendToPlayer(p, new Network.PhoneData(22, "{\"cut\":\"rift\",\"len\":50}"));
                p.sendSystemMessage(Component.literal("☄ Разлом открывается заново!").withStyle(ChatFormatting.LIGHT_PURPLE));
                continue;
            }
            Vec3 to = !WALLS.isEmpty() ? safeSpot() : new Vec3(C.getX() + 0.5 + 9, floorTop(), C.getZ() + 0.5);
            p.teleportTo(l, to.x, to.y, to.z, 90, 0); p.resetFallDistance();
            PacketDistributor.sendToPlayer(p, new Network.PhoneData(22, "{\"cut\":\"arrive\",\"len\":30}"));
            if (stage >= 1) PacketDistributor.sendToPlayer(p, new Network.PhoneData(31, fxJson(stage == 1 ? 1 : 2)));
            p.sendSystemMessage(Component.literal("☄ Ты снова в разломе!").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static void fail(ServerLevel l) {
        for (ServerPlayer o : l.getServer().getPlayerList().getPlayers())
            o.sendSystemMessage(Component.literal("☠ Демоница Пустоты смеётся: в разломе не осталось никого. Червь вернётся…").withStyle(ChatFormatting.DARK_PURPLE));
        end(l);
    }

    private static void end(ServerLevel l) {
        for (UUID u : new ArrayList<>(CHESTS.keySet())) drainChest(l, u);   // неподобранное — владельцу в сумку
        for (UUID u : MARKERS) { var e = l.getEntity(u); if (e != null) e.discard(); }
        MARKERS.clear();
        for (ServerPlayer p : fighters(l.getServer())) {
            BlockPos pad = com.pvpciv.floor.Belt.PAD;
            p.teleportTo(l, pad.getX() + 0.5, pad.getY(), pad.getZ() + 0.5, 180, 0);
            p.resetFallDistance();
        }
        clearBeams(l);
        clearWall(l);
        fx(l, 0);
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
        for (int y = C.getY() - 3; y <= C.getY() + 7; y++) clearLayer(l, y);
    }

    private static void say(ServerLevel l, String msg) {
        for (ServerPlayer p : fighters(l.getServer())) p.sendSystemMessage(Component.literal("☠ Демоница: " + msg).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Смерть бойца разлома — обычная смерть; он выбывает (снимется в tick). */
    public static void onPlayerDeath(ServerPlayer p) { if (bar != null) bar.removePlayer(p); }
}
