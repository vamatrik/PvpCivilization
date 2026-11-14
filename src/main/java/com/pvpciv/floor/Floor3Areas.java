package com.pvpciv.floor;

import com.pvpciv.combat.Mobs;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Локации этажа 3 вокруг библиотеки (0.9.0):
 *  • ВОСТОК — Ледяная тропа: узкая (3 блока) извилистая трасса без бортов над бездной → Деревня самураев (ур. 10–12),
 *    пруд братьев-капп, лавка повара Харуки, додзё.
 *  • ЗАПАД — Каменный путь: прямая дорога ~1900 блоков из скользкого камня; местами одна половина нескользкая —
 *    перестраивайся → Деревня Кумихо (ур. 13–15), святилище девятихвостой.
 * Тайники: деревня самураев — с 10 ур., Кумихо — с 13.
 */
public final class Floor3Areas extends Builder {
    public static final int Y = Floor3.Y;
    public static final BlockPos SV = new BlockPos(430, Y, 0), KV = new BlockPos(-1980, Y, 0);
    public static final int SV_R = 62, KV_R = 64, TRACK_X0 = 50, TRACK_X1 = SV.getX() - SV_R + 4, ROAD_X0 = -50, ROAD_X1 = KV.getX() + KV_R - 4;
    public static final BlockPos KAPPA_POND = SV.offset(30, 0, 24), KUMIHO_SHRINE = KV.offset(-34, 0, 0);
    public static final Vec3 CHEF = new Vec3(SV.getX() - 18.5, Y, SV.getZ() + 9.5);
    public static final List<BlockPos> STASHES = new ArrayList<>();
    public static final List<SpawnZones.Zone> ZONES = new ArrayList<>();
    public static final List<EliteSpots.Spot> ELITES = new ArrayList<>();

    static {
        // 0.10: мобы реже и шире (было 15–16 на деревню в тесных кругах — «огромная плотность»)
        ZONES.add(new SpawnZones.Zone("f3_sv_street", SV, 46, 5, 10, 12, 0f, Y, false, Mobs.Kind.SAMURAI, Mobs.Kind.NINJA, Mobs.Kind.ASHIGARU));
        ZONES.add(new SpawnZones.Zone("f3_sv_north", SV.offset(-6, 0, -42), 18, 3, 11, 12, 0f, Y, false, Mobs.Kind.NINJA, Mobs.Kind.NINJA, Mobs.Kind.SAMURAI));
        ZONES.add(new SpawnZones.Zone("f3_sv_east", SV.offset(40, 0, -14), 18, 3, 11, 12, 0f, Y, false, Mobs.Kind.SAMURAI, Mobs.Kind.ASHIGARU));
        ZONES.add(new SpawnZones.Zone("f3_kv_street", KV, 46, 5, 13, 15, 0f, Y, false, Mobs.Kind.WEREWOLF, Mobs.Kind.FOX, Mobs.Kind.KITSUNE_ARCHER));
        ZONES.add(new SpawnZones.Zone("f3_kv_grove", KV.offset(10, 0, -44), 18, 3, 14, 15, 0f, Y, false, Mobs.Kind.FOX, Mobs.Kind.FOX, Mobs.Kind.WEREWOLF));
        ZONES.add(new SpawnZones.Zone("f3_kv_south", KV.offset(18, 0, 42), 18, 3, 14, 15, 0f, Y, false, Mobs.Kind.KITSUNE_ARCHER, Mobs.Kind.WEREWOLF));
        ELITES.addAll(EliteSpots.twins(List.of(
                new EliteSpots.Spot("f3e_dojo", SV.offset(0, 0, -30), Mobs.Kind.SAMURAI, 12, "Мастер додзё", "floor3_mob", true),
                new EliteSpots.Spot("f3e_roof", SV.offset(-30, 0, -18), Mobs.Kind.NINJA, 12, "Тень на крыше", "floor3_mob", true),
                new EliteSpots.Spot("f3e_wolf", KV.offset(16, 0, -30), Mobs.Kind.WEREWOLF, 15, "Вожак стаи", "floor3b_mob", true),
                new EliteSpots.Spot("f3e_mask", KV.offset(20, 0, 28), Mobs.Kind.KITSUNE_ARCHER, 15, "Белая маска", "floor3b_mob", true))));
    }

    private Floor3Areas(ServerLevel level, long seed) { super(level, seed); }

    /** 0.9.9: подчистить «висящие» цветы и грибы во всех деревнях этажа 3. */
    public static void fixPlants(ServerLevel l) {
        int n = Decor3.fixPlants(l, KV.getX(), Y, KV.getZ(), KV_R + 4) + Decor3.fixPlants(l, SV.getX(), Y, SV.getZ(), SV_R + 4)
                + Decor3.fixPlants(l, 0, Y, 0, Floor3.ISLAND + 4);
        com.mojang.logging.LogUtils.getLogger().info("[pvpciv] этаж 3: убрано неустойчивых растений — {}", n);
    }

    public static void build(ServerLevel level, RandomSource r) {
        Floor3Areas b = new Floor3Areas(level, 909);
        STASHES.clear();
        b.track();
        b.road();
        b.samuraiVillage();
        b.kumihoVillage();
        b.fixShapes();
    }

    // ------------------------------------------------------------------ зоны
    public static final Zones.Zone TRACK = new Zones.Zone("Этаж 3 · Ледяная тропа", Zones.Type.SAFE);
    public static final Zones.Zone ROAD = new Zones.Zone("Этаж 3 · Каменный путь", Zones.Type.SAFE);
    public static final Zones.Zone SAMURAI = new Zones.Zone("Этаж 3 · Деревня самураев", Zones.Type.PVE);
    public static final Zones.Zone POND = new Zones.Zone("Этаж 3 · Пруд братьев-капп", Zones.Type.PVP);
    public static final Zones.Zone KUMIHO = new Zones.Zone("Этаж 3 · Деревня Кумихо", Zones.Type.PVE);
    public static final Zones.Zone SHRINE = new Zones.Zone("Этаж 3 · Святилище девяти хвостов", Zones.Type.PVP);

    public static Zones.Zone zone(Vec3 p, Zones.Zone fallback) {
        if (p.y < Y - 30) return fallback;
        if (Math.hypot(p.x - KAPPA_POND.getX(), p.z - KAPPA_POND.getZ()) < 16) return POND;
        if (Math.hypot(p.x - SV.getX(), p.z - SV.getZ()) < SV_R + 2) return SAMURAI;
        if (Math.hypot(p.x - KUMIHO_SHRINE.getX(), p.z - KUMIHO_SHRINE.getZ()) < 16) return SHRINE;
        if (Math.hypot(p.x - KV.getX(), p.z - KV.getZ()) < KV_R + 2) return KUMIHO;
        if (p.x > TRACK_X0 - 2 && p.x < TRACK_X1 + 2 && Math.abs(p.z - trackZ(p.x)) < 8) return TRACK;
        if (p.x < ROAD_X0 + 2 && p.x > ROAD_X1 - 2 && Math.abs(p.z) < 8) return ROAD;
        return fallback;
    }

    /** Тайники: деревня самураев — с 10 ур., деревня Кумихо — с 13. */
    public static int stashLevel(BlockPos pos) { return pos.getX() < 0 ? 13 : 10; }

    // ------------------------------------------------------------------ Ледяная тропа
    /** Ось трассы: резкие змейки, к середине — круче. */
    public static double trackZ(double x) {
        double t = (x - TRACK_X0);
        double amp = Math.min(1, t / 60) * Math.min(1, (TRACK_X1 - x) / 40);
        return amp * (24 * Math.sin(t / 26.0) + 9 * Math.sin(t / 8.5 + 1.3));
    }

    private void track() {
        for (double x = TRACK_X0; x <= TRACK_X1; x += 0.25) {
            double zc = trackZ(x);
            for (double w = -1.5; w <= 1.5; w += 0.5) {
                int bx = (int) Math.floor(x), bz = (int) Math.floor(zc + w);
                boolean edge = Math.abs(w) > 1.0;
                set(bx, Y - 1, bz, edge ? Blocks.PACKED_ICE : Blocks.BLUE_ICE);
                set(bx, Y - 2, bz, Blocks.PACKED_ICE);
            }
        }
        // площадки на концах и неоновые столбики-вехи каждые 40 блоков (без бортов!)
        for (int x = TRACK_X0 - 6; x <= TRACK_X0; x++) for (int z = -4; z <= 4; z++) set(x, Y - 1, z, Blocks.POLISHED_ANDESITE);
        for (int x = TRACK_X0; x < TRACK_X1; x += 40) {
            int z = (int) Math.round(trackZ(x)) + 3;
            set(x, Y - 1, z, Blocks.PACKED_ICE);
            set(x, Y, z, Blocks.SEA_LANTERN);
        }
        signPost(TRACK_X0 - 2, Y, 3, Direction.EAST, DyeColor.LIGHT_BLUE, Blocks.DARK_OAK_FENCE, "ЛЕДЯНАЯ ТРОПА", "к деревне", "самураев", "бортов нет!");
    }

    // ------------------------------------------------------------------ Каменный путь
    private void road() {
        int len = ROAD_X0 - ROAD_X1;
        for (int x = ROAD_X1; x <= ROAD_X0; x++) {
            int seg = Math.floorDiv(ROAD_X0 - x, 90);
            boolean rough = (seg % 3 == 1) && (ROAD_X0 - x) % 90 < 55;   // треть отрезков — с нескользкой половиной
            int roughSide = Math.floorMod(seg * 7 + 3, 2) == 0 ? 1 : -1;
            for (int z = -3; z <= 3; z++) {
                boolean slow = rough && Integer.signum(z) == roughSide;
                set(x, Y - 1, z, slow ? (((x + z) & 3) == 0 ? Blocks.COARSE_DIRT : Blocks.GRAVEL) : ModRegistry.SLICK_STONE.get());
                set(x, Y - 2, z, Blocks.STONE);
                if (Math.abs(z) == 3 && x % 2 == 0) set(x, Y - 2, z, Blocks.COBBLED_DEEPSLATE);
            }
            if ((ROAD_X0 - x) % 64 == 0) for (int s = -1; s <= 1; s += 2) {   // каменные фонари по краям
                set(x, Y - 1, s * 4, Blocks.STONE_BRICKS);
                set(x, Y, s * 4, Blocks.STONE_BRICK_WALL);
                setLit(x, Y + 1, s * 4, Blocks.LANTERN.defaultBlockState());
            }
        }
        signPost(ROAD_X0 + 2, Y, 4, Direction.WEST, DyeColor.ORANGE, Blocks.DARK_OAK_FENCE, "КАМЕННЫЙ ПУТЬ", "к деревне Кумихо", len + " блоков", "нужна лодка");
    }

    // ------------------------------------------------------------------ острова
    private void island(BlockPos c, int R, Block top, Block path) {
        int cx = c.getX(), cz = c.getZ();
        for (int x = -R - 3; x <= R + 3; x++) for (int z = -R - 3; z <= R + 3; z++) {
            double a = Math.atan2(z, x), r = Math.hypot(x, z), edge = R + 2 * Math.sin(a * 5) + 1.2 * Math.cos(a * 9);
            if (r > edge) continue;
            int depth = 4 + (int) ((edge - r) * 0.3) + rnd.nextInt(2);
            for (int d = 1; d <= depth; d++) set(cx + x, Y - d, cz + z, d == 1 ? top : d < 3 ? Blocks.DIRT : pick(Blocks.STONE, Blocks.ANDESITE, Blocks.TUFF));
            for (int y = Y; y <= Y + 16; y++) set(cx + x, y, cz + z, Blocks.AIR);
            if (Math.abs(x) <= 2 || Math.abs(z) <= 2) set(cx + x, Y - 1, cz + z, (x * 3 + z * 7) % 6 == 0 ? Blocks.MOSSY_STONE_BRICKS : path);
        }
    }

    /** Японский домик: каркас из тёмного дуба, белые стены-сёдзи, черепичная крыша. */
    private void house(int cx, int cz, int w, int d, Block wall, Block frame, Block roof, boolean stash) {
        for (int x = -w; x <= w; x++) for (int z = -d; z <= d; z++) {
            boolean edge = Math.abs(x) == w || Math.abs(z) == d, corner = Math.abs(x) == w && Math.abs(z) == d;
            set(cx + x, Y - 1, cz + z, Blocks.SPRUCE_PLANKS);
            for (int y = Y; y < Y + 4; y++) {
                if (!edge) { set(cx + x, y, cz + z, Blocks.AIR); continue; }
                boolean door = z == d && Math.abs(x) <= 0 && y < Y + 2;
                boolean win = !door && y == Y + 2 && !corner && (x + z) % 2 == 0;
                set(cx + x, y, cz + z, door ? Blocks.AIR.defaultBlockState() : corner || y == Y + 3 ? frame.defaultBlockState() : win ? Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState() : wall.defaultBlockState());
            }
        }
        // скатная крыша с выносом
        for (int k = 0; k <= d + 1; k++) for (int x = -w - 1; x <= w + 1; x++) {
            int y = Y + 4 + k / 2;
            for (int s = -1; s <= 1; s += 2) {
                int z = s * (d + 1 - k);
                set(cx + x, y, cz + z, roof);
            }
        }
        setLit(cx + 1, Y + 2, cz + d + 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(cx + 1, Y + 3, cz + d + 1, Blocks.DARK_OAK_FENCE);
        Decor3.interior(level, rnd, cx, Y, cz, w, d);
        Decor3.yard(level, rnd, cx, Y, cz, w, d);
        if (stash) stash(cx - w + 1, Y, cz - d + 1, STASHES);
    }

    private void samuraiVillage() {
        island(SV, SV_R, Blocks.GRASS_BLOCK, Blocks.STONE_BRICKS);
        int cx = SV.getX(), cz = SV.getZ();
        // дома вдоль двух улиц
        int[][] hs = {{-24, -12}, {-24, 14}, {16, -14}, {16, 12}, {-40, -2}, {-12, 30}, {12, -30}, {38, 2}, {-30, -30}, {30, -32}};
        for (int i = 0; i < hs.length; i++) house(cx + hs[i][0], cz + hs[i][1], 3, 3, Blocks.WHITE_CONCRETE, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.DEEPSLATE_TILES, i % 3 != 2);
        // додзё на севере
        house(cx, cz - 30, 6, 4, Blocks.WHITE_CONCRETE, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.DEEPSLATE_TILES, true);
        signPost(cx + 3, Y, cz - 24, Direction.SOUTH, DyeColor.RED, Blocks.DARK_OAK_FENCE, "ДОДЗЁ", "Мастер додзё", "не любит гостей", "");
        // лавка повара Харуки: прилавок и красные занавеси-норэн
        int sx = (int) Math.floor(CHEF.x), sz = (int) Math.floor(CHEF.z);
        for (int x = -3; x <= 3; x++) {
            set(sx + x, Y, sz - 1, Blocks.STRIPPED_SPRUCE_LOG);
            set(sx + x, Y + 1, sz - 1, x == 0 ? Blocks.SMOKER : Blocks.SPRUCE_SLAB);
            set(sx + x, Y + 3, sz - 2, Blocks.RED_WOOL);
            for (int z = -2; z <= 2; z++) set(sx + x, Y + 4, sz + z, Blocks.DARK_OAK_SLAB);
        }
        for (int[] p : new int[][]{{-3, -2}, {3, -2}, {-3, 2}, {3, 2}}) for (int y = Y; y < Y + 4; y++) set(sx + p[0], y, sz + p[1], Blocks.STRIPPED_DARK_OAK_LOG);
        set(sx, Y, sz, Blocks.AIR); set(sx, Y + 1, sz, Blocks.AIR);
        signPost(sx + 4, Y, sz - 3, Direction.NORTH, DyeColor.ORANGE, Blocks.DARK_OAK_FENCE, "ЛАВКА ХАРУКИ", "рамен · суши", "бэнто", "");
        // бамбуковая роща на северо-востоке
        for (int i = 0; i < 160; i++) {
            int x = cx + 20 + rnd.nextInt(26), z = cz - 46 + rnd.nextInt(22);
            if (Math.hypot(x - cx, z - cz) > SV_R - 4 || !level.getBlockState(new BlockPos(x, Y - 1, z)).is(Blocks.GRASS_BLOCK)) continue;
            int h = 5 + rnd.nextInt(6);
            for (int y = Y; y < Y + h; y++) set(x, y, z, Blocks.BAMBOO);
        }
        // сакура и каменные фонари
        for (int i = 0; i < 18; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 18 + rnd.nextDouble() * (SV_R - 24);
            int x = cx + (int) Math.round(Math.cos(a) * r), z = cz + (int) Math.round(Math.sin(a) * r);
            if (!level.getBlockState(new BlockPos(x, Y - 1, z)).is(Blocks.GRASS_BLOCK) || !air(x, Y, z)) continue;
            if (i % 2 == 0) { set(x, Y, z, Blocks.STONE_BRICK_WALL); set(x, Y + 1, z, Blocks.POLISHED_ANDESITE); setLit(x, Y + 2, z, Blocks.LANTERN.defaultBlockState()); continue; }
            for (int y = Y; y < Y + 4; y++) set(x, y, z, Blocks.CHERRY_LOG);
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = 3; dy <= 5; dy++)
                if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy - 4) <= 3 && air(x + dx, Y + dy, z + dz)) set(x + dx, Y + dy, z + dz, Blocks.CHERRY_LEAVES);
        }
        pond();
        // колодец на площади
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            int wx = cx + 8 + x, wz = cz - 8 + z;
            if (Math.abs(x) == 2 || Math.abs(z) == 2) { set(wx, Y, wz, Blocks.MOSSY_STONE_BRICKS); continue; }
            set(wx, Y - 1, wz, Blocks.WATER); set(wx, Y - 2, wz, Blocks.WATER);
        }
        for (int s = -1; s <= 1; s += 2) for (int y = Y + 1; y <= Y + 3; y++) set(cx + 8 + s * 2, y, cz - 8, Blocks.STRIPPED_DARK_OAK_LOG);
        for (int x = -2; x <= 2; x++) set(cx + 8 + x, Y + 4, cz - 8, Blocks.DEEPSLATE_TILE_SLAB);
        setLit(cx + 8, Y + 3, cz - 8, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // стойки с оружием у додзё и мишени для лучников
        for (int i = 0; i < 4; i++) { set(cx - 8 + i * 4, Y, cz - 22, Blocks.DARK_OAK_FENCE); set(cx - 8 + i * 4, Y + 1, cz - 22, Blocks.HAY_BLOCK); set(cx - 8 + i * 4, Y + 2, cz - 22, Blocks.TARGET); }
        Decor3.scatter(level, rnd, cx, Y, cz, SV_R, false);
        // въезд с тропы: площадка и тории
        int ex = cx - SV_R + 2;
        for (int x = ex - 4; x <= ex + 4; x++) for (int z = -4; z <= 4; z++) set(x, Y - 1, z, Blocks.POLISHED_ANDESITE);
        torii(ex + 6, 0, true);
        signPost(ex + 8, Y, 4, Direction.WEST, DyeColor.RED, Blocks.DARK_OAK_FENCE, "ДЕРЕВНЯ", "САМУРАЕВ", "ур. 10–12", "");
    }

    /** Пруд братьев-капп: вода, кувшинки, плоский берег-арена и огуречные грядки (морские огурцы). */
    private void pond() {
        int cx = KAPPA_POND.getX(), cz = KAPPA_POND.getZ();
        for (int x = -15; x <= 15; x++) for (int z = -15; z <= 15; z++) {
            double r = Math.hypot(x, z);
            if (r > 15) continue;
            for (int y = Y; y < Y + 6; y++) if (!level.getBlockState(new BlockPos(cx + x, y, cz + z)).is(Blocks.WATER)) set(cx + x, y, cz + z, Blocks.AIR);
            if (r <= 4.5) { set(cx + x, Y - 1, cz + z, Blocks.WATER); set(cx + x, Y - 2, cz + z, Blocks.WATER); set(cx + x, Y - 3, cz + z, Blocks.MUD); }
            else if (r <= 5.5) set(cx + x, Y - 1, cz + z, Blocks.MOSSY_COBBLESTONE);
            else set(cx + x, Y - 1, cz + z, r > 13.5 ? Blocks.MOSSY_STONE_BRICKS : (x + z) % 5 == 0 ? Blocks.MUD_BRICKS : Blocks.PACKED_MUD);
        }
        for (int i = 0; i < 6; i++) { double a = i * Math.PI / 3; setLit(cx + (int) Math.round(Math.cos(a) * 3), Y, cz + (int) Math.round(Math.sin(a) * 3), Blocks.LILY_PAD.defaultBlockState()); }
        for (int a = 0; a < 360; a += 60) {
            int x = cx + (int) Math.round(Math.cos(Math.toRadians(a)) * 13), z = cz + (int) Math.round(Math.sin(Math.toRadians(a)) * 13);
            set(x, Y, z, Blocks.STONE_BRICK_WALL); set(x, Y + 1, z, Blocks.POLISHED_ANDESITE); setLit(x, Y + 2, z, Blocks.SOUL_LANTERN.defaultBlockState());
        }
        signPost(cx - 14, Y, cz - 2, Direction.WEST, DyeColor.LIME, Blocks.DARK_OAK_FENCE, "ПРУД КАПП", "Братья выходят", "раз в 30 минут", "! PvP-зона !");
    }

    private void torii(int x, int z, boolean alongX) {
        for (int s = -1; s <= 1; s += 2) for (int y = Y; y < Y + 6; y++) set(alongX ? x : x + s * 3, y, alongX ? z + s * 3 : z, Blocks.RED_CONCRETE);
        for (int k = -4; k <= 4; k++) {
            set(alongX ? x : x + k, Y + 6, alongX ? z + k : z, Blocks.BLACK_CONCRETE);
            if (Math.abs(k) <= 3) set(alongX ? x : x + k, Y + 4, alongX ? z + k : z, Blocks.RED_CONCRETE);
        }
    }

    private void kumihoVillage() {
        island(KV, KV_R, Blocks.GRASS_BLOCK, Blocks.SMOOTH_SANDSTONE);
        int cx = KV.getX(), cz = KV.getZ();
        int[][] hs = {{20, -14}, {20, 14}, {-6, -24}, {-6, 24}, {36, 0}, {6, -40}, {6, 40}, {30, -30}, {30, 30}};
        for (int i = 0; i < hs.length; i++) house(cx + hs[i][0], cz + hs[i][1], 3, 3, Blocks.BIRCH_PLANKS, Blocks.STRIPPED_CHERRY_LOG, Blocks.RED_TERRACOTTA, i % 3 != 1);
        // аллея тории к святилищу
        for (int x = cx - 6; x >= KUMIHO_SHRINE.getX() + 14; x -= 4) torii(x, cz, true);
        // святилище: платформа, красные колонны, лисьи статуи
        int sx = KUMIHO_SHRINE.getX(), sz = KUMIHO_SHRINE.getZ();
        for (int x = -13; x <= 13; x++) for (int z = -13; z <= 13; z++) {
            double r = Math.hypot(x, z);
            if (r > 13) continue;
            set(sx + x, Y - 1, sz + z, r < 3 ? Blocks.GOLD_BLOCK : (x + z) % 2 == 0 ? Blocks.POLISHED_GRANITE : Blocks.SMOOTH_QUARTZ);
            for (int y = Y; y < Y + 8; y++) set(sx + x, y, sz + z, Blocks.AIR);
        }
        for (int a = 0; a < 360; a += 40) {
            int x = sx + (int) Math.round(Math.cos(Math.toRadians(a)) * 11), z = sz + (int) Math.round(Math.sin(Math.toRadians(a)) * 11);
            for (int y = Y; y < Y + 5; y++) set(x, y, z, Blocks.RED_CONCRETE);
            setLit(x, Y + 5, z, Blocks.SOUL_LANTERN.defaultBlockState());
        }
        for (int s = -1; s <= 1; s += 2) {   // лисьи статуи у входа
            int x = sx + 14, z = sz + s * 4;
            set(x, Y, z, Blocks.POLISHED_DIORITE); set(x, Y + 1, z, Blocks.WHITE_CONCRETE); set(x, Y + 2, z, Blocks.WHITE_CONCRETE);
            set(x, Y + 3, z, Blocks.ORANGE_CONCRETE);
        }
        signPost(sx + 15, Y, sz + 7, Direction.EAST, DyeColor.PINK, Blocks.DARK_OAK_FENCE, "СВЯТИЛИЩЕ", "Кумихо", "раз в 40 минут", "! PvP-зона !");
        // клёны (оранжевая листва — акация) и фонари
        for (int i = 0; i < 22; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 16 + rnd.nextDouble() * (KV_R - 22);
            int x = cx + (int) Math.round(Math.cos(a) * r), z = cz + (int) Math.round(Math.sin(a) * r);
            if (!level.getBlockState(new BlockPos(x, Y - 1, z)).is(Blocks.GRASS_BLOCK) || !air(x, Y, z) || Math.hypot(x - sx, z - sz) < 16) continue;
            for (int y = Y; y < Y + 4; y++) set(x, y, z, Blocks.DARK_OAK_LOG);
            Block leaf = i % 3 == 0 ? Blocks.CHERRY_LEAVES : Blocks.ACACIA_LEAVES;
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = 3; dy <= 5; dy++)
                if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy - 4) <= 3 && air(x + dx, Y + dy, z + dz)) set(x + dx, Y + dy, z + dz, leaf);
        }
        // лисьи норы-камни, тыквы и фонари-фонарики вдоль улицы
        for (int i = 0; i < 14; i++) {
            int x = cx - 30 + rnd.nextInt(80), z = cz - 50 + rnd.nextInt(100);
            if (!air(x, Y, z) || !level.getBlockState(new BlockPos(x, Y - 1, z)).is(Blocks.GRASS_BLOCK)) continue;
            set(x, Y, z, i % 3 == 0 ? Blocks.CARVED_PUMPKIN : i % 3 == 1 ? Blocks.MOSSY_COBBLESTONE : Blocks.PUMPKIN);
        }
        for (int x = cx - 4; x >= KUMIHO_SHRINE.getX() + 16; x -= 8) for (int s = -1; s <= 1; s += 2) {
            set(x, Y, s * 4, Blocks.STONE_BRICK_WALL); setLit(x, Y + 1, s * 4, Blocks.SOUL_LANTERN.defaultBlockState());
        }
        Decor3.scatter(level, rnd, cx, Y, cz, KV_R, true);
        // въезд с дороги: площадка с востока
        int ex = cx + KV_R - 2;
        for (int x = ex - 4; x <= ex + 4; x++) for (int z = -4; z <= 4; z++) set(x, Y - 1, z, Blocks.POLISHED_GRANITE);
        torii(ex - 6, 0, true);
        signPost(ex - 8, Y, 4, Direction.EAST, DyeColor.ORANGE, Blocks.DARK_OAK_FENCE, "ДЕРЕВНЯ", "КУМИХО", "ур. 13–15", "");
    }

    @SuppressWarnings("unused") private static final BlockState UNUSED = Blocks.AIR.defaultBlockState();
}
