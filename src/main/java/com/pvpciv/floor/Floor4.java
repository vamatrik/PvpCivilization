package com.pvpciv.floor;

import com.pvpciv.combat.Mobs;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import com.pvpciv.world.Builder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Этаж 4 мира 1 — НТЦ «NaZa» (0.11). Отдельное измерение pvpciv:world1_f4.
 *  • В центре — круглый вестибюль корпорации: стеклянная шахта лифта, ресепшен, уголок Чарователя (молекулярный стол), Стелла.
 *  • Четыре крытые стеклянные галереи ведут к куполам разных режимов сложности:
 *      север — Оранжерея (ур. 16–18), восток — Криолаборатория (18–20), юг — Реакторный купол (20–22), запад — Биокупол «Ковчег» (22–24).
 */
public final class Floor4 extends Builder {
    public static final int Y = 100;
    public static final int HUB_R = 18, ISLAND = 40, DOME_R = 30, DOME_D = 130;
    public static final Vec3 LIFT_ARRIVE = new Vec3(0.5, Y, 4.5);
    public static final BlockPos GREEN = new BlockPos(0, Y, -DOME_D), CRYO = new BlockPos(DOME_D, Y, 0), REACTOR = new BlockPos(0, Y, DOME_D), ARK = new BlockPos(-DOME_D, Y, 0);
    /** Молекулярный стол (Чарователь) и место Стеллы в вестибюле. */
    public static final BlockPos ENCH_TABLE = new BlockPos(11, Y, 0);
    public static final Vec3 ENCHANTER = new Vec3(14.5, Y, 3.5), STELLA = new Vec3(-12.5, Y, -6.5), RECEPTION = new Vec3(0.5, Y, -13.5);
    public static final BlockPos ARK_ARENA = ARK.offset(-12, 0, 0);
    public static final List<BlockPos> STASHES = new ArrayList<>();

    private Floor4(ServerLevel level) { super(level, 404); }

    public static void build(ServerLevel level) {
        Floor4 b = new Floor4(level);
        STASHES.clear();
        b.hub();
        for (int i = 0; i < 4; i++) b.gallery(i);
        b.dome(GREEN, Theme.GREEN);
        b.dome(CRYO, Theme.CRYO);
        b.dome(REACTOR, Theme.REACTOR);
        b.dome(ARK, Theme.ARK);
        b.fixShapes();
    }

    // ------------------------------------------------------------------ зоны
    public static final Zones.Zone HUB = new Zones.Zone("Этаж 4 · Вестибюль NaZa", Zones.Type.SAFE);
    public static final Zones.Zone GALLERY = new Zones.Zone("Этаж 4 · Галерея", Zones.Type.SAFE);
    public static final Zones.Zone Z_GREEN = new Zones.Zone("Этаж 4 · Оранжерея", Zones.Type.PVE);
    public static final Zones.Zone Z_CRYO = new Zones.Zone("Этаж 4 · Криолаборатория", Zones.Type.PVE);
    public static final Zones.Zone Z_REACTOR = new Zones.Zone("Этаж 4 · Реакторный купол", Zones.Type.PVE);
    public static final Zones.Zone Z_ARK = new Zones.Zone("Этаж 4 · Биокупол «Ковчег»", Zones.Type.PVP);
    public static final Zones.Zone SKY = new Zones.Zone("Небо над Шпилем", Zones.Type.SAFE);

    public static Zones.Zone zone(Vec3 p) {
        if (Math.hypot(p.x, p.z) <= ISLAND + 2) return HUB;
        if (near(p, GREEN)) return Z_GREEN;
        if (near(p, CRYO)) return Z_CRYO;
        if (near(p, REACTOR)) return Z_REACTOR;
        if (near(p, ARK)) return Z_ARK;
        if ((Math.abs(p.x) <= 4 || Math.abs(p.z) <= 4) && p.y > Y - 6) return GALLERY;
        return SKY;
    }
    private static boolean near(Vec3 p, BlockPos c) { return Math.hypot(p.x - c.getX(), p.z - c.getZ()) <= DOME_R + 6; }

    // ------------------------------------------------------------------ спавн
    public static final List<SpawnZones.Zone> ZONES = new ArrayList<>();
    static {
        ZONES.add(new SpawnZones.Zone("f4_green", GREEN, 24, 6, 16, 18, 0f, Y, false, Mobs.Kind.BOTANIST, Mobs.Kind.BOTANIST, Mobs.Kind.SPORELING));
        ZONES.add(new SpawnZones.Zone("f4_cryo", CRYO, 24, 6, 18, 20, 0f, Y, false, Mobs.Kind.THAWED, Mobs.Kind.THAWED, Mobs.Kind.SENTRY));
        ZONES.add(new SpawnZones.Zone("f4_reactor", REACTOR, 24, 6, 20, 22, 0f, Y, false, Mobs.Kind.ROBOT, Mobs.Kind.ROBOT, Mobs.Kind.TURRET));
        ZONES.add(new SpawnZones.Zone("f4_ark", ARK.offset(14, 0, 0), 14, 6, 22, 24, 0f, Y, false, Mobs.Kind.CHIMERA, Mobs.Kind.CHIMERA, Mobs.Kind.LAB_TECH));
    }

    public static final List<EliteSpots.Spot> ELITES = new ArrayList<>();
    static {
        ELITES.addAll(EliteSpots.twins(List.of(
                new EliteSpots.Spot("f4e_garden", GREEN.offset(14, 0, -10), Mobs.Kind.BOTANIST, 18, "Главный садовник", "floor4_mob", true),
                new EliteSpots.Spot("f4e_cryo", CRYO.offset(0, 0, 0).offset(8, 0, 12), Mobs.Kind.THAWED, 20, "Образец №0", "floor4_mob", true),
                new EliteSpots.Spot("f4e_core", REACTOR.offset(-12, 0, 10), Mobs.Kind.ROBOT, 22, "Страж ядра", "floor4_mob", true),
                new EliteSpots.Spot("f4e_alpha", ARK.offset(20, 0, -12), Mobs.Kind.CHIMERA, 24, "Альфа-химера", "floor4b_mob", true))));
    }

    /** Прибытие лифтом. */
    public static void onArrive(ServerPlayer p) {
        var d = com.pvpciv.progress.Progress.d(p);
        if (d.gates.contains("f4_seen")) return;
        d.gates.putInt("f4_seen", 1);
        p.setData(com.pvpciv.registry.ModRegistry.STORY.get(), d);
        p.sendSystemMessage(Component.literal("✦ Этаж 4 — НТЦ «NaZa». Четыре купола, четыре лаборатории, и все — без персонала. Чарователь Аурелий ждёт у молекулярного стола (восток вестибюля), Стелла — у инженерного (запад).").withStyle(ChatFormatting.AQUA));
        com.pvpciv.progress.Titles.grant(p, "naza_staff");
    }

    /** NPC вестибюля. */
    public static void ensureNpcs(net.minecraft.server.MinecraftServer server) {
        ServerLevel l = com.pvpciv.world.CivWorld.f4(server);
        if (!com.pvpciv.world.CivWorld.isF4(l)) return;
        Floor2.ensureNpc(l, com.pvpciv.registry.ModRegistry.ENCHANTER.get(), ENCHANTER, 90);
        Floor2.ensureNpc(l, com.pvpciv.registry.ModRegistry.STELLA.get(), STELLA, 225);
    }

    /** Таблица тайника по месту: «Ковчег» — богаче. */
    public static String stashTable(BlockPos p) { return "floor4_stash"; }

    // ------------------------------------------------------------------ стиль
    enum Theme {
        GREEN(Blocks.LIME_STAINED_GLASS, Blocks.WHITE_CONCRETE, Blocks.MOSS_BLOCK, Blocks.GLOWSTONE),
        CRYO(Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.WHITE_CONCRETE, Blocks.PACKED_ICE, Blocks.SEA_LANTERN),
        REACTOR(Blocks.ORANGE_STAINED_GLASS, Blocks.GRAY_CONCRETE, Blocks.CUT_COPPER, Blocks.REDSTONE_LAMP),
        ARK(Blocks.MAGENTA_STAINED_GLASS, Blocks.BLACK_CONCRETE, Blocks.PURPUR_BLOCK, Blocks.SHROOMLIGHT);
        final Block glass, frame, floor, light;
        Theme(Block g, Block f, Block fl, Block l) { glass = g; frame = f; floor = fl; light = l; }
    }

    private BlockState lit(Block b) {
        BlockState s = b.defaultBlockState();
        return b == Blocks.REDSTONE_LAMP ? s.setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true) : s;
    }

    // ------------------------------------------------------------------ вестибюль
    private void hub() {
        // остров-платформа: светлый бетон, кольца подсветки
        for (int x = -ISLAND - 2; x <= ISLAND + 2; x++) for (int z = -ISLAND - 2; z <= ISLAND + 2; z++) {
            double r = Math.hypot(x, z);
            double edge = ISLAND + Math.sin(Math.atan2(z, x) * 8) * 0.8;
            if (r > edge) continue;
            int depth = 3 + (int) ((edge - r) * 0.3);
            for (int d = 1; d <= depth; d++) set(x, Y - d, z, d == 1 ? (Math.abs(r - (ISLAND - 3)) < 0.6 ? Blocks.SEA_LANTERN : ((x + z) & 3) == 0 ? Blocks.LIGHT_GRAY_CONCRETE : Blocks.SMOOTH_STONE)
                    : d == depth ? Blocks.IRON_BLOCK : Blocks.GRAY_CONCRETE);
            for (int y = Y; y <= Y + 22; y++) set(x, y, z, Blocks.AIR);
        }
        // здание: круглое, стены — белый бетон с лентой голубого стекла и подсветкой
        for (int x = -HUB_R - 1; x <= HUB_R + 1; x++) for (int z = -HUB_R - 1; z <= HUB_R + 1; z++) {
            double r = Math.hypot(x, z);
            if (r <= HUB_R) set(x, Y - 1, z, r < 4 ? Blocks.WHITE_CONCRETE : ((int) r) % 5 == 0 ? Blocks.LIGHT_BLUE_CONCRETE : Blocks.QUARTZ_BLOCK);
            if (r > HUB_R - 0.5 && r <= HUB_R + 0.6) {
                for (int y = Y; y <= Y + 8; y++) {
                    Block b = y == Y || y == Y + 8 ? Blocks.WHITE_CONCRETE : y == Y + 7 ? Blocks.SEA_LANTERN : y >= Y + 2 && y <= Y + 5 ? Blocks.LIGHT_BLUE_STAINED_GLASS : Blocks.WHITE_CONCRETE;
                    set(x, y, z, b);
                }
            }
        }
        // купол-крыша (стекло, белые рёбра)
        for (int x = -HUB_R; x <= HUB_R; x++) for (int z = -HUB_R; z <= HUB_R; z++) {
            double r = Math.hypot(x, z);
            if (r > HUB_R + 0.5) continue;
            int h = (int) Math.round(Math.sqrt(Math.max(0, (HUB_R + 0.5) * (HUB_R + 0.5) - r * r)) * 0.45);
            boolean rib = Math.abs(x) <= 0 || Math.abs(z) <= 0 || Math.abs(Math.abs(x) - Math.abs(z)) <= 0;
            set(x, Y + 9 + h, z, rib ? Blocks.WHITE_CONCRETE : Blocks.LIGHT_BLUE_STAINED_GLASS);
        }
        // шахта лифта: стеклянный цилиндр, проём в полу до бездны
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            boolean edge = Math.abs(x) == 2 || Math.abs(z) == 2;
            for (int y = Y - 12; y < Y; y++) set(x, y, z, Blocks.AIR);
            if (edge && !(z == 2 && Math.abs(x) <= 1)) for (int y = Y; y <= Y + 4; y++) set(x, y, z, y == Y + 4 ? Blocks.WHITE_CONCRETE : Blocks.GLASS);
        }
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) if (Math.abs(x) == 2 || Math.abs(z) == 2) set(x, Y - 1, z, Blocks.SEA_LANTERN);
        // ресепшен (север): стойка, логотип NaZa
        for (int x = -5; x <= 5; x++) { set(x, Y, -12, Blocks.WHITE_CONCRETE); set(x, Y + 1, -12, Blocks.SMOOTH_QUARTZ_SLAB); }
        for (int x = -6; x <= 6; x++) for (int y = Y + 2; y <= Y + 6; y++) set(x, y, -17, Blocks.BLACK_CONCRETE);
        logo(-5, Y + 3, -16);
        // табличка с «расшифровкой» названия (шутка): официальная и нацарапанная поверх
        sign(new BlockPos(-3, Y + 1, -16), Direction.SOUTH, DyeColor.LIGHT_BLUE, "НТЦ «NaZa»", "Научно-Заводской", "Анклав", "основан в 20?? г.");
        sign(new BlockPos(3, Y + 1, -16), Direction.SOUTH, DyeColor.RED, "N a Z a", "НАЗАД", "дороги нет", "© уборщица");
        // уголок Чарователя (восток): молекулярный стол — стол зачарований на кварцевом постаменте, гнёзда модулей по кругу
        set(ENCH_TABLE.getX(), Y - 1, ENCH_TABLE.getZ(), Blocks.CRYING_OBSIDIAN);
        set(ENCH_TABLE.getX(), Y, ENCH_TABLE.getZ(), Blocks.ENCHANTING_TABLE);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int mx = ENCH_TABLE.getX() + (int) Math.round(Math.cos(a) * 3), mz = ENCH_TABLE.getZ() + (int) Math.round(Math.sin(a) * 3);
            set(mx, Y - 1, mz, Blocks.CYAN_CONCRETE);
            set(mx, Y, mz, Blocks.LIGHT_GRAY_CARPET);   // гнездо модуля (модуль ставится ПКМ — Enchanting.placeModule)
        }
        // уголок Стеллы (запад-север): инженерный стол, приборы
        set(-14, Y, -8, Blocks.SMITHING_TABLE); set(-13, Y, -9, Blocks.LODESTONE); set(-11, Y, -9, Blocks.LIGHTNING_ROD);
        set(-15, Y, -6, Blocks.AMETHYST_CLUSTER);
        // светильники и растения в кадках
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12 + 0.13;
            int x = (int) Math.round(Math.cos(a) * (HUB_R - 3)), z = (int) Math.round(Math.sin(a) * (HUB_R - 3));
            if (Math.abs(x) <= 4 || Math.abs(z) <= 4) continue;
            set(x, Y, z, rnd.nextBoolean() ? Blocks.AZALEA : Blocks.FLOWERING_AZALEA);
            set(x, Y - 1, z, Blocks.MOSS_BLOCK);
        }
        // четыре выхода (проёмы в стене) к галереям
        for (int i = 0; i < 4; i++) {
            int[] d = DIRS[i];
            for (int w = -2; w <= 2; w++) for (int y = Y; y <= Y + 4; y++) {
                int x = d[0] * HUB_R + d[1] * w, z = d[1] * HUB_R + d[0] * w;
                set(x, y, z, Blocks.AIR);
                set(x + d[0], y, z + d[1], Blocks.AIR);
            }
        }
    }

    private static final int[][] DIRS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};   // север, восток, юг, запад
    private static final BlockPos[] DOMES = {GREEN, CRYO, REACTOR, ARK};

    /** Надпись NaZa из блоков (5 букв по 3 ширины). */
    private void logo(int x0, int y0, int z) {
        String[] rows = {"X.X.XXX.XXX.XXX", "XXX.X.X..X..X.X", "X.X.XXX.X...XXX", "X.X.X.X.XXX.X.X"};
        for (int r = 0; r < rows.length; r++) for (int c = 0; c < rows[r].length(); c++)
            if (rows[r].charAt(c) == 'X') set(x0 + c - 2, y0 + 3 - r, z, c < 4 || c >= 8 && c < 11 ? Blocks.LIGHT_BLUE_CONCRETE : Blocks.CYAN_CONCRETE);
    }

    // ------------------------------------------------------------------ галереи
    private void gallery(int i) {
        int[] d = DIRS[i];
        int from = HUB_R + 1, to = DOME_D - DOME_R - 1;
        for (int t = from; t <= to; t++) {
            for (int w = -3; w <= 3; w++) {
                int x = d[0] * t + d[1] * w, z = d[1] * t + d[0] * w;
                boolean wall = Math.abs(w) == 3;
                set(x, Y - 1, z, wall ? Blocks.WHITE_CONCRETE : Math.abs(w) == 0 && t % 4 == 0 ? Blocks.SEA_LANTERN : Blocks.SMOOTH_QUARTZ);
                for (int y = Y; y <= Y + 4; y++) set(x, y, z, wall ? (y == Y ? Blocks.WHITE_CONCRETE : Blocks.GLASS) : Blocks.AIR);
                set(x, Y + 5, z, wall ? Blocks.WHITE_CONCRETE : (t % 6 == 0 ? Blocks.WHITE_CONCRETE : Blocks.GLASS));
            }
            if (t % 12 == 0) for (int y = Y - 30; y < Y - 1; y++) { set(d[0] * t, y, d[1] * t, Blocks.IRON_BARS); }
        }
    }

    // ------------------------------------------------------------------ купола
    private void dome(BlockPos c, Theme th) {
        int cx = c.getX(), cz = c.getZ();
        int pr = DOME_R + 3;
        for (int x = -pr; x <= pr; x++) for (int z = -pr; z <= pr; z++) {
            double r = Math.hypot(x, z);
            if (r > pr) continue;
            for (int d = 1; d <= 4; d++) set(cx + x, Y - d, cz + z, d == 1 ? (r > DOME_R ? th.frame : floorOf(th, x, z, r)) : d == 4 ? Blocks.IRON_BLOCK : Blocks.GRAY_CONCRETE);
            for (int y = Y; y <= Y + DOME_R; y++) set(cx + x, y, cz + z, Blocks.AIR);
        }
        // полусфера: стекло, рёбра — рама
        for (int x = -DOME_R; x <= DOME_R; x++) for (int z = -DOME_R; z <= DOME_R; z++) for (int y = 0; y <= DOME_R; y++) {
            double r = Math.sqrt(x * x + z * z + y * y * 1.6 * 1.6);
            if (Math.abs(r - DOME_R) > 0.55) continue;
            double ang = Math.atan2(z, x);
            boolean rib = Math.abs(Math.sin(ang * 6)) < 0.08 || y % 6 == 0;
            set(cx + x, Y + y, cz + z, rib ? th.frame : th.glass);
        }
        // вход со стороны галереи
        int[] d = DIRS[java.util.Arrays.asList(DOMES).indexOf(c)];
        for (int t = DOME_R - 4; t <= DOME_R + 3; t++) for (int w = -2; w <= 2; w++) for (int y = Y; y <= Y + 4; y++)
            set(cx - d[0] * t + d[1] * w, y, cz - d[1] * t + d[0] * w, Blocks.AIR);
        switch (th) {
            case GREEN -> greenhouse(cx, cz);
            case CRYO -> cryo(cx, cz);
            case REACTOR -> reactor(cx, cz);
            case ARK -> ark(cx, cz);
        }
        // светильники по кругу
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16;
            int x = cx + (int) Math.round(Math.cos(a) * (DOME_R - 3)), z = cz + (int) Math.round(Math.sin(a) * (DOME_R - 3));
            set(x, Y - 1, z, lit(th.light));
        }
        // тайники
        for (int i = 0; i < 3; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            BlockPos s = new BlockPos(cx + (int) (Math.cos(a) * (DOME_R - 7)), Y, cz + (int) (Math.sin(a) * (DOME_R - 7)));
            stash(s.getX(), Y, s.getZ(), STASHES);
        }
    }

    private Block floorOf(Theme th, int x, int z, double r) {
        return switch (th) {
            case GREEN -> ((x * 3 + z * 5) & 7) == 0 ? Blocks.ROOTED_DIRT : r % 9 < 1 ? Blocks.MOSSY_STONE_BRICKS : Blocks.MOSS_BLOCK;
            case CRYO -> ((x + z) & 1) == 0 ? Blocks.PACKED_ICE : Blocks.SNOW_BLOCK;
            case REACTOR -> ((x / 3 + z / 3) & 1) == 0 ? Blocks.CUT_COPPER : Blocks.POLISHED_DEEPSLATE;
            case ARK -> r % 6 < 1 ? Blocks.PURPUR_PILLAR : ((x + z) & 3) == 0 ? Blocks.MAGENTA_TERRACOTTA : Blocks.PURPUR_BLOCK;
        };
    }

    private void greenhouse(int cx, int cz) {
        // водные каналы крестом, деревья-азалии, лианы, кусты
        for (int t = -DOME_R + 4; t <= DOME_R - 4; t++) { if (Math.abs(t) < 4) continue; set(cx + t, Y - 1, cz + 6, Blocks.WATER); set(cx - 6, Y - 1, cz + t, Blocks.WATER); }
        for (int i = 0; i < 14; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 6 + rnd.nextDouble() * (DOME_R - 10);
            tree(cx + (int) (Math.cos(a) * r), cz + (int) (Math.sin(a) * r));
        }
        for (int i = 0; i < 120; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = rnd.nextDouble() * (DOME_R - 3);
            int x = cx + (int) (Math.cos(a) * r), z = cz + (int) (Math.sin(a) * r);
            if (level.getBlockState(new BlockPos(x, Y, z)).isAir() && level.getBlockState(new BlockPos(x, Y - 1, z)).is(Blocks.MOSS_BLOCK))
                set(x, Y, z, pick(Blocks.FERN, Blocks.SHORT_GRASS, Blocks.MOSS_CARPET, Blocks.FLOWERING_AZALEA, Blocks.ALLIUM, Blocks.BLUE_ORCHID));
        }
    }

    private void tree(int x, int z) {
        int h = 4 + rnd.nextInt(3);
        for (int y = 0; y < h; y++) set(x, Y + y, z, Blocks.OAK_LOG);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = -1; dy <= 1; dy++) {
            if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy) > 3) continue;
            BlockPos p = new BlockPos(x + dx, Y + h + dy, z + dz);
            if (level.getBlockState(p).isAir()) set(p.getX(), p.getY(), p.getZ(), (rnd.nextInt(3) == 0 ? Blocks.FLOWERING_AZALEA_LEAVES : Blocks.AZALEA_LEAVES).defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        }
    }

    private void cryo(int cx, int cz) {
        // ряды криокапсул: синий лёд в стеклянных колбах, пульты
        for (int row = -2; row <= 2; row++) for (int k = -3; k <= 3; k++) {
            if (Math.abs(row) == 0 && Math.abs(k) <= 1) continue;
            int x = cx + k * 6, z = cz + row * 7;
            if (Math.hypot(x - cx, z - cz) > DOME_R - 5) continue;
            set(x, Y, z, Blocks.IRON_BLOCK);
            set(x, Y + 1, z, Blocks.BLUE_ICE); set(x, Y + 2, z, Blocks.BLUE_ICE);
            for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) { set(x + o[0], Y + 1, z + o[1], Blocks.GLASS_PANE); set(x + o[0], Y + 2, z + o[1], Blocks.GLASS_PANE); }
            set(x, Y + 3, z, Blocks.SEA_LANTERN);
            set(x + 1, Y, z + 1, Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA);
        }
        // центр: большой криосаркофаг
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = 0; y < 4; y++) set(cx + x, Y + y, cz + z, y == 3 ? Blocks.SEA_LANTERN : Math.abs(x) + Math.abs(z) == 0 ? Blocks.BLUE_ICE : Blocks.LIGHT_BLUE_STAINED_GLASS);
        for (int i = 0; i < 60; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = rnd.nextDouble() * (DOME_R - 3);
            int x = cx + (int) (Math.cos(a) * r), z = cz + (int) (Math.sin(a) * r);
            if (level.getBlockState(new BlockPos(x, Y, z)).isAir()) set(x, Y, z, Blocks.SNOW);
        }
    }

    private void reactor(int cx, int cz) {
        // ядро реактора: колонна магмы и морских фонарей за решёткой, кольца трубопроводов
        for (int y = 0; y < 14; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            int m = Math.max(Math.abs(x), Math.abs(z));
            if (m == 2) set(cx + x, Y + y, cz + z, (y % 4 == 0) ? Blocks.WAXED_OXIDIZED_CUT_COPPER : Blocks.IRON_BARS);
            else set(cx + x, Y + y, cz + z, (x == 0 && z == 0) ? Blocks.SEA_LANTERN : Blocks.MAGMA_BLOCK);
        }
        for (int ring = 9; ring <= 21; ring += 6) for (int i = 0; i < ring * 8; i++) {
            double a = Math.PI * 2 * i / (ring * 8);
            int x = cx + (int) Math.round(Math.cos(a) * ring), z = cz + (int) Math.round(Math.sin(a) * ring);
            set(x, Y + 2 + (ring - 9) / 3, z, Blocks.WAXED_CUT_COPPER);
        }
        for (int i = 0; i < 10; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 6 + rnd.nextDouble() * (DOME_R - 9);
            int x = cx + (int) (Math.cos(a) * r), z = cz + (int) (Math.sin(a) * r);
            set(x, Y, z, Blocks.IRON_BLOCK); set(x, Y + 1, z, lit(Blocks.REDSTONE_LAMP)); set(x, Y + 2, z, Blocks.LIGHTNING_ROD);
        }
    }

    private void ark(int cx, int cz) {
        // биокупол: колбы с «образцами», лужи слизи, круглая арена на западе (финальный босс)
        int ax = ARK_ARENA.getX(), az = ARK_ARENA.getZ();
        for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
            double r = Math.hypot(x, z);
            if (r <= 12) set(ax + x, Y - 1, az + z, r > 11 ? Blocks.CRYING_OBSIDIAN : r < 2.5 ? Blocks.MOSS_BLOCK : ((x + z) & 1) == 0 ? Blocks.BLACK_CONCRETE : Blocks.GRAY_CONCRETE);
        }
        for (int i = 0; i < 8; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 4 + rnd.nextDouble() * 10;
            int x = cx + 14 + (int) (Math.cos(a) * r * 0.6), z = cz + (int) (Math.sin(a) * r);
            set(x, Y, z, Blocks.POLISHED_BLACKSTONE);
            for (int y = 1; y <= 3; y++) set(x, Y + y, z, y == 3 ? Blocks.SHROOMLIGHT : Blocks.PURPLE_STAINED_GLASS);
        }
        for (int i = 0; i < 30; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = rnd.nextDouble() * (DOME_R - 3);
            int x = cx + (int) (Math.cos(a) * r), z = cz + (int) (Math.sin(a) * r);
            if (level.getBlockState(new BlockPos(x, Y, z)).isAir()) set(x, Y - 1, z, Blocks.SLIME_BLOCK);
        }
    }
}
