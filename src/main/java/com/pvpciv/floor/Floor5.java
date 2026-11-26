package com.pvpciv.floor;

import com.pvpciv.combat.Mobs;
import com.pvpciv.world.Builder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Этаж 5 мира 1 — орбитальная станция «Орбита» (0.12). Измерение pvpciv:world1_f5 (тип pvpciv:space — космическое небо).
 *
 * <p>Станция строится ГЕНЕРАТОРОМ с фиксированным зерном: во всех мирах она одинаковая, но собрана процедурно.
 * Сетка 7×7 ячеек по {@link #CELL} блоков; из центра растёт остов (рандомизированный алгоритм Прима) + несколько петель,
 * каждой ячейке назначается тип модуля, модули соединены коридорами. Уровни мобов растут от центра к краям (24…32).
 * Поменять станцию = поменять {@link #SEED} или правила ниже; повторная постройка даёт тот же результат.
 */
public final class Floor5 extends Builder {
    public static final long SEED = 5005L;
    public static final int Y = 100, N = 7, CELL = 46, ROOM = 15, H = 9;
    /** Центральная ячейка (атриум): лифт (сломан), Луна, врата. */
    public static final int C = N / 2;
    public static final List<BlockPos> STASHES = new ArrayList<>();

    public enum Type {
        ATRIUM("Атриум", Zones.Type.SAFE), HANGAR("Ангар", Zones.Type.SAFE), BRIDGE("Мостик", Zones.Type.PVP),
        HABITAT("Жилой модуль", Zones.Type.PVE), HYDRO("Гидропоника", Zones.Type.PVE), LAB("Лаборатория", Zones.Type.PVE),
        CARGO("Грузовой отсек", Zones.Type.PVE), ENGINE("Машинное отделение", Zones.Type.PVE), OBSERVATORY("Обсерватория", Zones.Type.PVE),
        MEDBAY("Медотсек", Zones.Type.PVE);
        public final String title; public final Zones.Type zone;
        Type(String t, Zones.Type z) { title = t; zone = z; }
    }

    /** Модуль станции: ячейка сетки, тип, уровень мобов. */
    public record Module(int gx, int gz, Type type, int level) {
        public BlockPos center() { return new BlockPos((gx - C) * CELL, Y, (gz - C) * CELL); }
        public String id() { return "f5_" + gx + "_" + gz; }
    }

    // ------------------------------------------------------------------ план станции (детерминированный)
    public static final List<Module> MODULES = new ArrayList<>();
    /** Рёбра (коридоры): пары индексов ячеек gx*N+gz. */
    public static final List<int[]> LINKS = new ArrayList<>();
    public static Module HUB, HANGAR_M, BRIDGE_M;

    static { plan(); }

    private static void plan() {
        Random r = new Random(SEED);
        boolean[][] used = new boolean[N][N];
        used[C][C] = true;
        List<int[]> frontier = new ArrayList<>();
        int[][] D = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : D) frontier.add(new int[]{C, C, C + d[0], C + d[1]});
        int target = 27;
        int count = 1;
        while (count < target && !frontier.isEmpty()) {
            int[] e = frontier.remove(r.nextInt(frontier.size()));
            int x = e[2], z = e[3];
            if (x < 0 || z < 0 || x >= N || z >= N || used[x][z]) continue;
            // не даём станции «расползтись» к углам — круглее
            if (Math.hypot(x - C, z - C) > 3.3) continue;
            used[x][z] = true;
            count++;
            LINKS.add(new int[]{e[0] * N + e[1], x * N + z});
            for (int[] d : D) frontier.add(new int[]{x, z, x + d[0], z + d[1]});
        }
        // петли: несколько лишних коридоров между соседями
        for (int k = 0; k < 6; k++) {
            int x = r.nextInt(N), z = r.nextInt(N);
            if (!used[x][z]) { k--; continue; }
            int[] d = D[r.nextInt(4)];
            int x2 = x + d[0], z2 = z + d[1];
            if (x2 < 0 || z2 < 0 || x2 >= N || z2 >= N || !used[x2][z2]) continue;
            int a = x * N + z, b = x2 * N + z2;
            boolean dup = false;
            for (int[] l : LINKS) if (l[0] == a && l[1] == b || l[0] == b && l[1] == a) dup = true;
            if (!dup) LINKS.add(new int[]{a, b});
        }
        // типы: ангар — самый южный, мостик — самый северный, остальное — случайно с весами
        int hx = -1, hz = -1, bx = -1, bz = N;
        for (int x = 0; x < N; x++) for (int z = 0; z < N; z++) if (used[x][z]) {
            if (z > hz || z == hz && Math.abs(x - C) < Math.abs(hx - C)) { hx = x; hz = z; }
            if (z < bz || z == bz && Math.abs(x - C) < Math.abs(bx - C)) { bx = x; bz = z; }
        }
        // 1.2: уровень модуля — по пути от ангара (туда прилетает ракета): чем дальше по коридорам, тем сильнее (было — по кольцу от центра)
        int[] dist = new int[N * N];
        java.util.Arrays.fill(dist, -1);
        int[] deg = new int[N * N];
        for (int[] l : LINKS) { deg[l[0]]++; deg[l[1]]++; }
        java.util.ArrayDeque<Integer> q = new java.util.ArrayDeque<>();
        dist[hx * N + hz] = 0; q.add(hx * N + hz);
        int maxD = 1;
        while (!q.isEmpty()) {
            int u = q.poll();
            for (int[] l : LINKS) {
                int v = l[0] == u ? l[1] : l[1] == u ? l[0] : -1;
                if (v < 0 || dist[v] >= 0) continue;
                dist[v] = dist[u] + 1; maxD = Math.max(maxD, dist[v]); q.add(v);
            }
        }
        Type[] pool = {Type.HABITAT, Type.HABITAT, Type.HYDRO, Type.HYDRO, Type.LAB, Type.LAB, Type.CARGO, Type.CARGO, Type.ENGINE, Type.OBSERVATORY, Type.MEDBAY};
        for (int x = 0; x < N; x++) for (int z = 0; z < N; z++) {
            if (!used[x][z]) continue;
            Type t = x == C && z == C ? Type.ATRIUM : x == hx && z == hz ? Type.HANGAR : x == bx && z == bz ? Type.BRIDGE : pool[r.nextInt(pool.length)];
            int ring = Math.max(Math.abs(x - C), Math.abs(z - C));
            int jitter = r.nextInt(2);
            int dd = Math.max(0, dist[x * N + z]);
            int lvl = Math.min(30, 23 + (int) Math.round(dd * 6.0 / maxD) + (deg[x * N + z] == 1 ? 1 : 0) + (dd > 1 ? jitter : 0));
            if (ring < 0) lvl = 23;
            Module m = new Module(x, z, t, lvl);
            MODULES.add(m);
            if (t == Type.ATRIUM) HUB = m;
            if (t == Type.HANGAR) HANGAR_M = m;
            if (t == Type.BRIDGE) BRIDGE_M = m;
        }
    }

    public static Module at(int gx, int gz) { for (Module m : MODULES) if (m.gx == gx && m.gz == gz) return m; return null; }

    /** 1.1: малая ракета ангара — низ по центру и блоки относительно него. */
    public static BlockPos ROCKET() { return HANGAR_M.center().offset(0, 0, 4); }
    /** 1.2: пульт в кабине ракеты (от {@link #ROCKET()}); люк — (0, 4..5, −2). */
    public static final BlockPos ROCKET_CONSOLE_OFF = new BlockPos(0, 4, 1);
    public static final BlockPos[] ROCKET_HATCH = {new BlockPos(0, 4, -2), new BlockPos(0, 5, -2)};
    public static java.util.Map<BlockPos, BlockState> rocketVoxels() {
        java.util.Map<BlockPos, BlockState> m = new java.util.LinkedHashMap<>();
        for (int y = 0; y < 12; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            double r = Math.hypot(x, z), rr = y < 8 ? 2.4 : 2.4 * (1 - (y - 7) / 5.0);
            if (r > rr || (r < rr - 1.1 && y > 0 && y < 11)) continue;
            m.put(new BlockPos(x, y, z), (y >= 10 ? Blocks.RED_CONCRETE : y % 4 == 2 ? Blocks.BLACK_CONCRETE : Blocks.WHITE_CONCRETE).defaultBlockState());
        }
        // 1.2: лестница снаружи, люк, пол кабины и пульт внутри
        m.remove(new BlockPos(0, 4, -2)); m.remove(new BlockPos(0, 5, -2));
        for (int y = 0; y <= 3; y++) m.put(new BlockPos(0, y, -3), Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.NORTH));
        for (int[] c : new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}}) m.put(new BlockPos(c[0], 3, c[1]), Blocks.IRON_BLOCK.defaultBlockState());
        m.put(ROCKET_CONSOLE_OFF, com.pvpciv.block.ConsoleBlock.of(com.pvpciv.block.ConsoleBlock.Kind.STATION, Direction.NORTH));
        return m;
    }

    public static Vec3 HANGAR_ARRIVE() { BlockPos c = HANGAR_M.center(); return new Vec3(c.getX() + 0.5, Y, c.getZ() - 6.5); }
    /** Кнопка-пульт ракеты в ангаре. */
    public static BlockPos HANGAR_CONSOLE() { return HANGAR_M.center().offset(5, 0, -9); }
    /** 1.0: пульт шлюза в ангаре (выход к поясу астероидов) и пульт лифта в атриуме. */
    public static BlockPos AIRLOCK() { return HANGAR_M.center().offset(-5, 0, ROOM - 3); }
    public static BlockPos LIFT_CONSOLE() { return HUB.center().offset(2, 0, -4); }
    public static Vec3 LIFT_ARRIVE() { BlockPos c = HUB.center(); return new Vec3(c.getX() + 0.5, Y, c.getZ() - 5.5); }
    public static Vec3 LUNA() { BlockPos c = HUB.center(); return new Vec3(c.getX() + 8.5, Y, c.getZ() - 8.5); }

    private Floor5(ServerLevel l) { super(l, SEED); }

    public static void build(ServerLevel level) {
        Floor5 b = new Floor5(level);
        STASHES.clear();
        for (int[] l : LINKS) b.corridor(l[0], l[1]);
        for (Module m : MODULES) b.module(m);
        for (int[] l : LINKS) b.doors(l[0], l[1]);
        b.fixShapes();
        Belt.build(level);   // 1.0: пояс астероидов
        com.pvpciv.game.Ritual.build(level);   // 1.1: ритуальный зал
        // 1.2: телепорты-площадки «Орбиты» убраны — только врата
    }

    // ------------------------------------------------------------------ зоны и спавн
    public static final Zones.Zone CORRIDOR = new Zones.Zone("Этаж 5 · Коридор", Zones.Type.PVE);
    public static final Zones.Zone RITUAL_Z = new Zones.Zone("Ритуальный зал", Zones.Type.SAFE);
    public static final Zones.Zone OUTSIDE = new Zones.Zone("Открытый космос", Zones.Type.SAFE);
    private static final Map<Type, Zones.Zone> ZONE_BY = new EnumMap<>(Type.class);
    static { for (Type t : Type.values()) ZONE_BY.put(t, new Zones.Zone("Этаж 5 · " + t.title, t.zone)); }

    public static final Zones.Zone LUNA_ARENA = new Zones.Zone("Арена Луны", Zones.Type.PVE), BELT_Z = new Zones.Zone("Пояс астероидов", Zones.Type.PVE),
            RIFT_Z = new Zones.Zone("Разлом Пустоты", Zones.Type.PVE);
    public static Zones.Zone zone(Vec3 p) {
        if (p.x > com.pvpciv.game.LunaTrial.AX - 200) return LUNA_ARENA;
        if (p.z > com.pvpciv.game.VoidRift.C.getZ() - 200) return RIFT_Z;
        if (p.z > Belt.CENTER.getZ() - 140) return BELT_Z;
        if (p.x > com.pvpciv.game.Ritual.X0 - 0.5 && p.x < com.pvpciv.game.Ritual.X1 + 1 && p.z > com.pvpciv.game.Ritual.Z0 && p.z < com.pvpciv.game.Ritual.Z1 + 1 && p.y > Y - 3 && p.y < Y + 12) return RITUAL_Z;
        int gx = (int) Math.round(p.x / CELL) + C, gz = (int) Math.round(p.z / CELL) + C;
        Module m = at(gx, gz);
        if (m != null) {
            BlockPos c = m.center();
            if (Math.abs(p.x - c.getX()) <= ROOM + 1 && Math.abs(p.z - c.getZ()) <= ROOM + 1) return ZONE_BY.get(m.type);
        }
        if (p.y > Y - 4 && p.y < Y + H + 2) return CORRIDOR;
        return OUTSIDE;
    }

    public static final List<SpawnZones.Zone> ZONES = new ArrayList<>();
    static {
        for (Module m : MODULES) {
            Mobs.Kind[] k = switch (m.type) {
                case HABITAT -> new Mobs.Kind[]{Mobs.Kind.ROGUE_ASTRO, Mobs.Kind.ROGUE_ASTRO, Mobs.Kind.COLLECTOR};
                case HYDRO -> new Mobs.Kind[]{Mobs.Kind.XENO, Mobs.Kind.VOID_CRAWLER, Mobs.Kind.VOID_CRAWLER};
                case LAB -> new Mobs.Kind[]{Mobs.Kind.XENO, Mobs.Kind.XENO, Mobs.Kind.VOID_CRAWLER, Mobs.Kind.ANDROID};   // 1.0.1: стрелков меньше
                case CARGO -> new Mobs.Kind[]{Mobs.Kind.COLLECTOR, Mobs.Kind.ROGUE_ASTRO, Mobs.Kind.ROGUE_ASTRO, Mobs.Kind.ANDROID};
                case ENGINE -> new Mobs.Kind[]{Mobs.Kind.DRONE, Mobs.Kind.ROGUE_ASTRO, Mobs.Kind.VOID_CRAWLER, Mobs.Kind.ANDROID};
                case OBSERVATORY -> new Mobs.Kind[]{Mobs.Kind.VOID_CRAWLER, Mobs.Kind.XENO, Mobs.Kind.VOID_CRAWLER, Mobs.Kind.DRONE};
                case MEDBAY -> new Mobs.Kind[]{Mobs.Kind.ROGUE_ASTRO, Mobs.Kind.XENO, Mobs.Kind.COLLECTOR};
                default -> null;
            };
            if (k == null) continue;
            ZONES.add(new SpawnZones.Zone(m.id(), m.center(), ROOM - 3, 6, Math.max(24, m.level - 1), m.level, 0f, Y, false, k));
        }
    }

    public static final List<EliteSpots.Spot> ELITES = new ArrayList<>();
    static {
        List<EliteSpots.Spot> l = new ArrayList<>();
        int i = 0;
        for (Module m : MODULES) {
            if (m.type.zone != Zones.Type.PVE || m.level < 27 || i >= 5) continue;
            Mobs.Kind k = switch (m.type) { case ENGINE -> Mobs.Kind.VOID_CRAWLER; case HYDRO, OBSERVATORY -> Mobs.Kind.XENO; case CARGO -> Mobs.Kind.COLLECTOR; default -> Mobs.Kind.ROGUE_ASTRO; };
            String[] names = {"Старпом-зомби", "Королева улья", "Главный коллектор", "Прототип «Страж»", "Капитан без шлема"};
            l.add(new EliteSpots.Spot("f5e_" + i, m.center().offset(6, 0, 6), k, m.level + 1, names[i % names.length], "floor5_mob", true));
            i++;
        }
        ELITES.addAll(EliteSpots.twins(l));
    }

    public static void onArrive(ServerPlayer p) {
        var d = com.pvpciv.progress.Progress.d(p);
        if (d.gates.contains("f5_seen")) return;
        d.gates.putInt("f5_seen", 1);
        p.setData(com.pvpciv.registry.ModRegistry.STORY.get(), d);
        p.sendSystemMessage(Component.literal("✦ Этаж 5 — орбитальная станция «Орбита». Атриум в центре: там Луна. Ангар — на юге, мостик — на севере. Окна не открывать.").withStyle(ChatFormatting.LIGHT_PURPLE));
        com.pvpciv.progress.Titles.grant(p, "orbit");
    }

    private static boolean logged;
    public static void ensureNpcs(net.minecraft.server.MinecraftServer server) {
        if (!logged) { logged = true; com.mojang.logging.LogUtils.getLogger().info("[PvpCiv] F5: hub {} hangar {} bridge {}", HUB.center(), HANGAR_M.center(), BRIDGE_M.center()); }
        ServerLevel l = com.pvpciv.world.CivWorld.f5(server);
        if (!com.pvpciv.world.CivWorld.isF5(l)) return;
        Floor2.ensureNpc(l, com.pvpciv.registry.ModRegistry.LUNA_STATION.get(), LUNA(), 225);
    }

    // ------------------------------------------------------------------ палитра
    private static final BlockState HULL = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), PANEL = Blocks.WHITE_CONCRETE.defaultBlockState(),
            TRIM = Blocks.CYAN_TERRACOTTA.defaultBlockState(), DARK = Blocks.GRAY_CONCRETE.defaultBlockState(), FLOOR = Blocks.POLISHED_ANDESITE.defaultBlockState(),
            GRID = Blocks.IRON_BLOCK.defaultBlockState(), LAMP = Blocks.SEA_LANTERN.defaultBlockState(), WIN = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(),
            AIR = Blocks.AIR.defaultBlockState();

    private BlockState lamp(Block b) {
        BlockState s = b.defaultBlockState();
        return b == Blocks.REDSTONE_LAMP ? s.setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true) : s;
    }

    // ------------------------------------------------------------------ коридоры
    private void corridor(int a, int b) {
        int ax = a / N, az = a % N, bx = b / N, bz = b % N;
        int x0 = (ax - C) * CELL, z0 = (az - C) * CELL, x1 = (bx - C) * CELL, z1 = (bz - C) * CELL;
        boolean alongX = az == bz;
        int from = (alongX ? Math.min(x0, x1) : Math.min(z0, z1)) + ROOM, to = (alongX ? Math.max(x0, x1) : Math.max(z0, z1)) - ROOM;
        int fixed = alongX ? z0 : x0;
        for (int t = from; t <= to; t++) for (int w = -3; w <= 3; w++) {
            int x = alongX ? t : fixed + w, z = alongX ? fixed + w : t;
            boolean wall = Math.abs(w) == 3;
            set(x, Y - 1, z, wall ? HULL : w == 0 ? (t % 6 == 0 ? LAMP : TRIM) : GRID);
            set(x, Y - 2, z, DARK);
            for (int y = Y; y <= Y + 4; y++) set(x, y, z, wall ? (y == Y + 2 && t % 5 != 0 ? WIN : PANEL) : AIR);
            set(x, Y + 5, z, wall ? HULL : (t % 6 == 3 && w == 0 ? LAMP : PANEL));
            set(x, Y + 6, z, DARK);
            if (wall && t % 9 == 0) set(x + (alongX ? 0 : Integer.signum(w)), Y + 2, z + (alongX ? Integer.signum(w) : 0), Blocks.LIGHTNING_ROD);   // антенны снаружи
        }
    }

    /** Проёмы в стенах модулей на месте коридоров. */
    private void doors(int a, int b) {
        int ax = a / N, az = a % N, bx = b / N, bz = b % N;
        for (int[] m : new int[][]{{ax, az, bx - ax, bz - az}, {bx, bz, ax - bx, az - bz}}) {
            int cx = (m[0] - C) * CELL, cz = (m[1] - C) * CELL;
            for (int w = -2; w <= 2; w++) for (int y = Y; y <= Y + 4; y++) for (int k = 0; k <= 1; k++) {
                int x = m[2] != 0 ? cx + m[2] * (ROOM + k) : cx + w, z = m[3] != 0 ? cz + m[3] * (ROOM + k) : cz + w;
                set(x, y, z, AIR);
            }
            for (int w = -3; w <= 3; w += 6) for (int y = Y; y <= Y + 4; y++) {   // рамка шлюза
                int x = m[2] != 0 ? cx + m[2] * ROOM : cx + w, z = m[3] != 0 ? cz + m[3] * ROOM : cz + w;
                set(x, y, z, y == Y + 4 ? lamp(Blocks.REDSTONE_LAMP) : Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            }
        }
    }

    // ------------------------------------------------------------------ модули
    private void module(Module m) {
        BlockPos c = m.center();
        int cx = c.getX(), cz = c.getZ();
        int R = ROOM, hh = m.type == Type.ATRIUM || m.type == Type.HANGAR || m.type == Type.OBSERVATORY ? H + 5 : H;
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
            boolean edge = Math.abs(x) == R || Math.abs(z) == R, corner = Math.abs(x) >= R - 1 && Math.abs(z) >= R - 1;
            set(cx + x, Y - 2, cz + z, DARK);
            set(cx + x, Y - 1, cz + z, edge ? HULL : floor(m.type, x, z));
            for (int y = Y; y < Y + hh; y++) {
                BlockState s = AIR;
                if (edge) s = corner ? HULL : (y - Y) % 4 == 2 && (Math.abs(x) + Math.abs(z)) % 4 != 0 ? WIN : (y == Y ? TRIM : PANEL);
                set(cx + x, y, cz + z, s);
            }
            boolean light = (Math.abs(x) % 6 == 3) && (Math.abs(z) % 6 == 3);
            set(cx + x, Y + hh, cz + z, edge ? HULL : light ? LAMP : m.type == Type.OBSERVATORY && Math.hypot(x, z) < R - 2 ? WIN : PANEL);
            set(cx + x, Y + hh + 1, cz + z, DARK);
        }
        // снаружи: солнечные панели или антенна на крыше
        if (rnd.nextInt(3) == 0 && m.type != Type.OBSERVATORY) for (int x = -R + 2; x <= R - 2; x++) for (int z = -3; z <= 3; z++)
            set(cx + x, Y + hh + 2, cz + z, (x + 50) % 5 == 0 ? Blocks.IRON_BLOCK.defaultBlockState() : Blocks.BLUE_STAINED_GLASS.defaultBlockState());
        else for (int y = 1; y < 7; y++) set(cx + 4, Y + hh + 1 + y, cz - 4, y == 6 ? lamp(Blocks.REDSTONE_LAMP) : Blocks.IRON_BARS.defaultBlockState());
        switch (m.type) {
            case ATRIUM -> atrium(cx, cz);
            case HANGAR -> hangar(cx, cz);
            case BRIDGE -> bridge(cx, cz);
            case HABITAT -> habitat(cx, cz);
            case HYDRO -> hydro(cx, cz);
            case LAB -> lab(cx, cz);
            case CARGO -> cargo(cx, cz);
            case ENGINE -> engine(cx, cz);
            case OBSERVATORY -> observatory(cx, cz);
            case MEDBAY -> medbay(cx, cz);
        }
        if (m.type.zone == Zones.Type.PVE) for (int i = 0; i < 2; i++) {
            int x = cx + (rnd.nextBoolean() ? 1 : -1) * (R - 2), z = cz + rnd.nextInt(2 * R - 6) - R + 3;
            if (rnd.nextBoolean()) { int t = x; x = cx + (z - cz); z = cz + (t - cx); }
            stash(x, Y, z, STASHES);
        }
        sign(new BlockPos(cx, Y + 3, cz - R + 1), Direction.SOUTH, DyeColor.CYAN, "СТАНЦИЯ «ОРБИТА»", m.type.title, m.type.zone == Zones.Type.PVE ? "ур. " + Math.max(24, m.level - 1) + "–" + m.level : "", "");
    }

    private BlockState floor(Type t, int x, int z) {
        return switch (t) {
            case HYDRO -> ((x + z) & 1) == 0 ? Blocks.MOSS_BLOCK.defaultBlockState() : GRID;
            case ENGINE -> ((x / 2 + z / 2) & 1) == 0 ? Blocks.WAXED_CUT_COPPER.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            case MEDBAY -> ((x + z) & 3) == 0 ? Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState() : Blocks.WHITE_CONCRETE.defaultBlockState();
            case BRIDGE -> Math.abs(x) <= 1 ? Blocks.BLACK_CONCRETE.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            case ATRIUM -> Math.hypot(x, z) % 5 < 1 ? Blocks.CYAN_CONCRETE.defaultBlockState() : Blocks.SMOOTH_QUARTZ.defaultBlockState();
            default -> ((x + z) & 3) == 0 ? GRID : FLOOR;
        };
    }

    private void atrium(int cx, int cz) {
        // шахта лифта в центре: сломана (искрит), кольцо-площадка
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            double r = Math.hypot(x, z);
            if (r > 3.5) continue;
            set(cx + x, Y - 1, cz + z, r < 2 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : LAMP);
        }
        for (int y = 0; y < 13; y++) for (int[] o : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) set(cx + o[0], Y + y, cz + o[1], Blocks.IRON_BARS.defaultBlockState());
        sign(new BlockPos(cx, Y + 1, cz - 4), Direction.NORTH, DyeColor.RED, "НЕБЕСНЫЙ ЛИФТ", "ПКМ по пульту", "ремонт: нужен", "«адский след»");
        BlockPos lk = LIFT_CONSOLE();
        set(lk.getX(), Y, lk.getZ(), com.pvpciv.block.ConsoleBlock.of(com.pvpciv.block.ConsoleBlock.Kind.STATION, Direction.NORTH));
        // уголок Луны: диванчик, витрина, неоновая вывеска
        for (int x = 5; x <= 11; x++) set(cx + x, Y, cz - 11, Blocks.PURPLE_WOOL.defaultBlockState());
        for (int x = 6; x <= 10; x++) { set(cx + x, Y, cz - 6, Blocks.SMOOTH_QUARTZ.defaultBlockState()); set(cx + x, Y + 1, cz - 6, Blocks.PINK_STAINED_GLASS_PANE.defaultBlockState()); }
        sign(new BlockPos(cx + 8, Y + 3, cz - 12), Direction.SOUTH, DyeColor.PINK, "ЛАВКА ЛУНЫ", "открыто всегда", "(почти)", "");
        // деревья в кадках и лавки
        for (int[] o : new int[][]{{-9, -9}, {9, 9}, {-9, 9}}) { set(cx + o[0], Y - 1, cz + o[1], Blocks.MOSS_BLOCK.defaultBlockState()); set(cx + o[0], Y, cz + o[1], Blocks.FLOWERING_AZALEA.defaultBlockState()); }
        for (int z = -4; z <= 4; z++) { set(cx - 12, Y, cz + z, Blocks.SMOOTH_QUARTZ_STAIRS.defaultBlockState()); }
    }

    private void hangar(int cx, int cz) {
        // посадочная площадка с разметкой и ракета «Заря-5» (копия — на ней летали)
        for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
            double r = Math.hypot(x, z);
            if (r > 10) continue;
            set(cx + x, Y - 1, cz + z + 2, Math.abs(r - 9) < 0.6 || Math.abs(r - 4) < 0.6 ? Blocks.YELLOW_CONCRETE.defaultBlockState() : Blocks.GRAY_CONCRETE.defaultBlockState());
        }
        int rz = cz + 4;
        for (var e : rocketVoxels().entrySet()) set(cx + e.getKey().getX(), Y + e.getKey().getY(), rz + e.getKey().getZ(), e.getValue());
        // шлюз наружу (на юг): двойные ворота из решёток — в 0.15 отсюда полёт на пояс астероидов
        for (int x = -3; x <= 3; x++) for (int y = 0; y < 6; y++) set(cx + x, Y + y, cz + ROOM, y == 5 ? lamp(Blocks.REDSTONE_LAMP) : Blocks.IRON_BARS.defaultBlockState());
        // 1.0: пульт шлюза
        BlockPos al = AIRLOCK();
        set(al.getX(), Y, al.getZ(), com.pvpciv.block.ConsoleBlock.of(com.pvpciv.block.ConsoleBlock.Kind.STATION, Direction.NORTH));
        sign(al.above(2), Direction.NORTH, DyeColor.LIGHT_BLUE, "ШЛЮЗ", "ПКМ по пульту —", "пояс астероидов", "(ключ: Дипломат)");
        // пульт ракеты
        BlockPos k = HANGAR_CONSOLE();
        set(k.getX(), Y, k.getZ(), com.pvpciv.block.ConsoleBlock.of(com.pvpciv.block.ConsoleBlock.Kind.STATION, Direction.NORTH));
        set(k.getX() - 1, Y, k.getZ(), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        set(k.getX() + 1, Y, k.getZ(), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        sign(k.above(2), Direction.NORTH, DyeColor.ORANGE, "РАКЕТА «ЗАРЯ-5»", "ПКМ по пульту —", "полёт на этаж 4", "(космодром NaZa)");
    }

    private void bridge(int cx, int cz) {
        // мостик капитана — будущая арена Дипломата (0.14): пульты полукругом, обзорное окно на север
        for (int i = -5; i <= 5; i++) {
            double a = Math.PI * (0.15 + 0.7 * (i + 5) / 10.0);
            int x = cx + (int) Math.round(Math.cos(a) * 10), z = cz - (int) Math.round(Math.sin(a) * 10);
            set(x, Y, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            set(x, Y + 1, z, i % 2 == 0 ? Blocks.OBSERVER.defaultBlockState() : lamp(Blocks.REDSTONE_LAMP));
        }
        for (int x = -10; x <= 10; x++) for (int y = Y + 1; y < Y + H - 1; y++) set(cx + x, y, cz - ROOM, WIN);
        set(cx, Y, cz + 4, Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState());   // кресло капитана
    }

    private void habitat(int cx, int cz) {
        for (int i = 0; i < 6; i++) {   // койки в два яруса у стен
            int x = cx - 12 + i * 4, z = cz - 12;
            set(x, Y, z, Blocks.WHITE_BED.defaultBlockState());
            set(x, Y + 2, z, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState());
            set(x + 1, Y, z, Blocks.BARREL.defaultBlockState());
        }
        for (int x = -3; x <= 3; x++) for (int z = -2; z <= 2; z++) if (Math.abs(x) == 3 || Math.abs(z) == 2) set(cx + x, Y, cz + z + 6, Blocks.WHITE_CARPET.defaultBlockState());
        set(cx, Y, cz + 6, Blocks.SMOOTH_QUARTZ.defaultBlockState()); set(cx, Y + 1, cz + 6, Blocks.FLOWER_POT.defaultBlockState());
    }

    private void hydro(int cx, int cz) {
        for (int row = -2; row <= 2; row++) for (int t = -11; t <= 11; t++) {   // гряды и трубы полива
            if (Math.abs(t) < 2) continue;
            int x = cx + t, z = cz + row * 5;
            set(x, Y, z, Blocks.FARMLAND.defaultBlockState());
            set(x, Y + 1, z, (rnd.nextInt(3) == 0 ? Blocks.WHEAT : rnd.nextBoolean() ? Blocks.CARROTS : Blocks.BEETROOTS).defaultBlockState());
            set(x, Y + 4, z, Blocks.GLASS_PANE.defaultBlockState());
            if (t % 4 == 0) set(x, Y + 5, z, Blocks.SHROOMLIGHT.defaultBlockState());
        }
        for (int t = -11; t <= 11; t++) set(cx + t, Y - 1, cz + 12, Blocks.WATER.defaultBlockState());
    }

    private void lab(int cx, int cz) {
        for (int i = 0; i < 5; i++) {   // колбы с образцами
            int x = cx - 10 + i * 5, z = cz - 9;
            set(x, Y, z, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
            for (int y = 1; y <= 3; y++) set(x, Y + y, z, (i % 2 == 0 ? Blocks.LIME_STAINED_GLASS : Blocks.PURPLE_STAINED_GLASS).defaultBlockState());
            set(x, Y + 4, z, Blocks.SEA_LANTERN.defaultBlockState());
        }
        for (int x = -6; x <= 6; x++) { set(cx + x, Y, cz + 3, Blocks.SMOOTH_QUARTZ.defaultBlockState()); if (x % 3 == 0) set(cx + x, Y + 1, cz + 3, Blocks.BREWING_STAND.defaultBlockState()); }
    }

    private void cargo(int cx, int cz) {
        for (int i = 0; i < 26; i++) {   // штабели контейнеров
            int x = cx + rnd.nextInt(25) - 12, z = cz + rnd.nextInt(25) - 12;
            if (Math.abs(x - cx) < 3 || Math.abs(z - cz) < 3) continue;
            Block b = pick(Blocks.ORANGE_SHULKER_BOX, Blocks.LIGHT_GRAY_SHULKER_BOX, Blocks.BLUE_SHULKER_BOX, Blocks.BARREL);
            int hgt = 1 + rnd.nextInt(3);
            for (int y = 0; y < hgt; y++) set(x, Y + y, z, b);
        }
    }

    private void engine(int cx, int cz) {
        for (int y = 0; y < H - 1; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {   // реактор
            int m = Math.max(Math.abs(x), Math.abs(z));
            set(cx + x, Y + y, cz + z, m == 2 ? (y % 3 == 0 ? Blocks.WAXED_OXIDIZED_CUT_COPPER : Blocks.IRON_BARS).defaultBlockState() : (x == 0 && z == 0 ? Blocks.SEA_LANTERN : Blocks.MAGENTA_STAINED_GLASS).defaultBlockState());
        }
        for (int t = -12; t <= 12; t++) { set(cx + t, Y + 6, cz - 10, Blocks.WAXED_CUT_COPPER.defaultBlockState()); set(cx - 10, Y + 6, cz + t, Blocks.WAXED_CUT_COPPER.defaultBlockState()); }
    }

    private void observatory(int cx, int cz) {
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) if (Math.hypot(x, z) < 6) set(cx + x, Y - 1, cz + z, WIN);   // стеклянный пол — под ногами космос
        set(cx, Y, cz, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        set(cx, Y + 1, cz, Blocks.LIGHTNING_ROD.defaultBlockState());
        set(cx + 1, Y + 2, cz, Blocks.IRON_BARS.defaultBlockState());
    }

    private void medbay(int cx, int cz) {
        for (int i = 0; i < 4; i++) {
            int x = cx - 9 + i * 6, z = cz + 9;
            set(x, Y, z, Blocks.WHITE_BED.defaultBlockState());
            set(x + 1, Y, z, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE.defaultBlockState());
        }
        for (int x = -2; x <= 2; x++) set(cx + x, Y, cz - 8, Blocks.QUARTZ_PILLAR.defaultBlockState());
        set(cx, Y + 1, cz - 8, Blocks.RED_STAINED_GLASS_PANE.defaultBlockState());
    }
}
