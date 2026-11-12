package com.pvpciv.floor;

import com.pvpciv.combat.Mobs;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Этаж 2 мира 1 — «Неоновый город» (измерение pvpciv:world1_f2). Город-кольцо вокруг хаба:
 * в центре — станция Небесного лифта и зал Тимофея; 12 радиальных улиц, внутренний и средний проспекты,
 * площади (Неоновая, Рынок, Парк), клуб «Серра» (логово Дона, PvP), небоскрёбы с неоном, у края — променад с перилами.
 * Под проспектами — канализация (локация 2): кольцевой сток, 8 радиальных туннелей, люки, Тронный сток Крысиного короля.
 */
public class Floor2 extends Builder {
    public static final int Y = 100, YS = 88;                 // улица / пол канализации (уровни ходьбы)
    public static final int HUB_R = 26, R_EDGE = 150;
    public static final double MID = 89.5;
    public static final BlockPos LIFT_PANEL = new BlockPos(2, Y, 3);
    public static final Vec3 LIFT_ARRIVE = new Vec3(0.5, Y, 5.5);
    public static final BlockPos NEON = polar(45, 120, Y), MARKET = polar(135, 120, Y), PARK = polar(225, 120, Y);
    public static final BlockPos CLUB = polar(315, 120, Y);
    public static final int CLUB_R = 15, PLAZA_R = 17;
    public static final BlockPos RAT_THRONE = polar(180, 58, YS);
    public static final int RAT_R = 15;
    public static final BlockPos GYM = new BlockPos(0, Y, -16), PLUMBER = polar(0, 124, YS);
    /** Логово контрабандиста: конец северного туннеля, перед ним — паркур над лавой. */
    public static final double DEN_DEG = 270;
    public static double denEnd() { return edge(Math.toRadians(DEN_DEG)) - 10; }
    public static BlockPos den() { return polar(DEN_DEG, denEnd() - 3, YS); }
    public static final BlockPos TOWER = polar(15, 62, Y);    // небоскрёб «Альтаир» с парашютистом на крыше
    public static final int TOWER_H = 58;
    public static final List<BlockPos> STASHES = new ArrayList<>(), SEWER_STASHES = new ArrayList<>();

    public record Place(String name, BlockPos c, int r) {}
    public static final List<Place> PLACES = List.of(new Place("Неоновая площадь", NEON, PLAZA_R), new Place("Рынок", MARKET, PLAZA_R),
            new Place("Парк", PARK, PLAZA_R), new Place("Небоскрёб «Альтаир»", TOWER, 10));

    public static BlockPos polar(double deg, double r, int y) {
        double a = Math.toRadians(deg);
        return new BlockPos((int) Math.round(Math.cos(a) * r), y, (int) Math.round(Math.sin(a) * r));
    }

    public static double edge(double a) { return R_EDGE + 6 * Math.sin(5 * a) + 3 * Math.sin(11 * a + 1); }

    // ------------------------------------------------------------------ мобы
    public static final List<SpawnZones.Zone> ZONES = new ArrayList<>();
    public static final List<EliteSpots.Spot> ELITES = new ArrayList<>();
    static {
        Mobs.Kind[][] street = {{Mobs.Kind.POLICE, Mobs.Kind.THUG}, {Mobs.Kind.THUG, Mobs.Kind.STREET_DOG}, {Mobs.Kind.POLICE, Mobs.Kind.STREET_DOG, Mobs.Kind.THUG}};
        for (int i = 0; i < 12; i++) {
            double deg = i * 30;
            ZONES.add(SpawnZones.Zone.f2("f2_in" + i, polar(deg, 60, Y), 16, 3, 6, 7, street[i % 3]));
            ZONES.add(SpawnZones.Zone.f2("f2_out" + i, polar(deg + 15, 118, Y), 18, 4, 7, 8, street[(i + 1) % 3]));
        }
        for (int i = 0; i < 8; i++) {
            ZONES.add(SpawnZones.Zone.f2("f2s_ring" + i, polar(i * 45 + 22.5, MID, YS), 12, 3, 8, 9, Mobs.Kind.RAT, Mobs.Kind.RAT, Mobs.Kind.GHOUL));
            ZONES.add(SpawnZones.Zone.f2("f2s_rad" + i, polar(i * 45, 125, YS), 10, 2, 9, 10, i % 3 == 0 ? new Mobs.Kind[]{Mobs.Kind.GUNNER, Mobs.Kind.GHOUL} : new Mobs.Kind[]{Mobs.Kind.RAT, Mobs.Kind.GHOUL}));
        }
        ELITES.add(new EliteSpots.Spot("e2_neon", NEON.offset(6, 0, 4), Mobs.Kind.POLICE, 8, "Сержант Кобальт", "floor2_mob", true));
        ELITES.add(new EliteSpots.Spot("e2_market", MARKET.offset(-5, 0, 6), Mobs.Kind.THUG, 8, "Барыга Сеня", "floor2_mob", true));
        ELITES.add(new EliteSpots.Spot("e2_park", PARK.offset(4, 0, -6), Mobs.Kind.STREET_DOG, 8, "Вожак стаи", "floor2_mob", true));
        ELITES.add(new EliteSpots.Spot("e2_sewer1", polar(90, MID, YS), Mobs.Kind.GHOUL, 10, "Утопленник", "floor2b_mob", true));
        ELITES.add(new EliteSpots.Spot("e2_sewer2", polar(270, MID, YS), Mobs.Kind.GUNNER, 10, "Ствол", "floor2b_mob", true));
        List<EliteSpots.Spot> tw = EliteSpots.twins(new ArrayList<>(ELITES));
        ELITES.clear(); ELITES.addAll(tw);
    }

    // ------------------------------------------------------------------ зоны HUD
    public static final Zones.Zone HUB2 = new Zones.Zone("Этаж 2 · Станция лифта", Zones.Type.SAFE);
    public static final Zones.Zone CITY = new Zones.Zone("Этаж 2 · Неоновый город", Zones.Type.PVE);
    public static final Zones.Zone CLUB_Z = new Zones.Zone("Этаж 2 · Клуб «Серра»", Zones.Type.PVP);
    public static final Zones.Zone SEWER = new Zones.Zone("Этаж 2 · Канализация", Zones.Type.PVE);
    public static final Zones.Zone THRONE_Z = new Zones.Zone("Этаж 2 · Тронный сток", Zones.Type.PVP);
    public static final Zones.Zone SKY = new Zones.Zone("Небо над Шпилем", Zones.Type.SAFE);

    public static Zones.Zone zone(Vec3 p) {
        double r = Math.hypot(p.x, p.z);
        if (p.y < YS + 7 && p.y > YS - 6) {
            if (dist(p.x, p.z, RAT_THRONE.getX() + 0.5, RAT_THRONE.getZ() + 0.5) <= RAT_R + 2) return THRONE_Z;
            if (dist(p.x, p.z, den().getX() + 0.5, den().getZ() + 0.5) <= 6) return new Zones.Zone(SEWER.name() + " · Логово контрабандиста", Zones.Type.SAFE);
            return SEWER;
        }
        if (r <= HUB_R + 6) return HUB2;
        if (r > edge(Math.atan2(p.z, p.x)) + 2 || p.y < YS - 10) return SKY;
        if (dist(p.x, p.z, CLUB.getX() + 0.5, CLUB.getZ() + 0.5) <= CLUB_R + 2) return CLUB_Z;
        for (Place pl : PLACES) if (dist(p.x, p.z, pl.c().getX() + 0.5, pl.c().getZ() + 0.5) <= pl.r()) return new Zones.Zone(CITY.name() + " · " + pl.name(), Zones.Type.PVE);
        return CITY;
    }

    public static int stashLevel(BlockPos p) { return p.getY() < YS + 6 ? 9 : 6; }
    public static String stashTable(BlockPos p) { return p.getY() < YS + 6 ? "floor2b_stash" : "floor2_stash"; }

    // ------------------------------------------------------------------ стройка
    private Floor2(ServerLevel level) { super(level, 2022); }

    public static void build(ServerLevel level) {
        Floor2 b = new Floor2(level);
        STASHES.clear(); SEWER_STASHES.clear();
        b.ground();
        b.sewer();
        b.towers();
        b.hub();
        b.plazas();
        b.club();
        b.props();
        b.fixShapes();
    }

    private static double angDiff(double a, double b) { double d = Math.abs(a - b) % 360; return d > 180 ? 360 - d : d; }

    /** Расстояние до ближайшей радиальной улицы (лучи через каждые 30°). */
    private static double radialDist(double x, double z) {
        double r = Math.hypot(x, z), deg = Math.toDegrees(Math.atan2(z, x));
        double near = Math.round(deg / 30.0) * 30.0;
        return r * Math.sin(Math.toRadians(angDiff(deg, near)));
    }

    /** Тип клетки поверхности: 0 — вне, 1 — хаб, 2 — дорога, 3 — тротуар, 4 — площадь, 5 — участок под застройку, 6 — променад. */
    private static int cell(double x, double z) {
        double r = Math.hypot(x, z), e = edge(Math.atan2(z, x));
        if (r > e) return 0;
        if (r <= HUB_R) return 1;
        for (BlockPos c : new BlockPos[]{NEON, MARKET, PARK, CLUB}) if (dist(x, z, c.getX(), c.getZ()) <= PLAZA_R) return 4;
        if (r > e - 6) return 6;
        double rd = radialDist(x, z);
        double ring = Math.min(Math.abs(r - 29.5), Math.abs(r - MID));
        if (rd < 3.5 || ring < 3.5) return 2;
        if (rd < 5.5 || ring < 5.5) return 3;
        return 5;
    }

    private void ground() {
        for (int x = -R_EDGE - 10; x <= R_EDGE + 10; x++) for (int z = -R_EDGE - 10; z <= R_EDGE + 10; z++) {
            int c = cell(x, z);
            if (c == 0) continue;
            set(x, Y - 2, z, Blocks.STONE);
            BlockState top = switch (c) {
                case 1 -> ((x + z) & 3) == 0 ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : Blocks.WHITE_CONCRETE.defaultBlockState();
                case 2 -> {
                    double rd = radialDist(x, z), r = Math.hypot(x, z);
                    boolean dash = (rd < 0.5 && ((int) r & 7) < 4) || ((Math.abs(r - 29.5) < 0.5 || Math.abs(r - MID) < 0.5) && ((x * 3 + z * 7) & 7) < 3);
                    yield dash ? ModRegistry.NEON_ROAD_LINE.get().defaultBlockState() : ModRegistry.NEON_ROAD.get().defaultBlockState();
                }
                case 3 -> ((x + z) & 1) == 0 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState();
                case 4 -> ((x * x + z * z) / 9 & 1) == 0 ? Blocks.POLISHED_ANDESITE.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
                case 6 -> Blocks.POLISHED_DIORITE.defaultBlockState();
                default -> Blocks.GRAY_CONCRETE.defaultBlockState();
            };
            set(x, Y - 1, z, top);
            // перила по краю
            double r = Math.hypot(x, z), e = edge(Math.atan2(z, x));
            if (r > e - 1.2 && ((x * 31 + z * 17) % 23 != 0)) set(x, Y, z, Blocks.IRON_BARS);
        }
    }

    // ------------------------------------------------------------------ канализация
    private static boolean tunnel(double x, double z, double w) {
        double r = Math.hypot(x, z), e = edge(Math.atan2(z, x));
        if (Math.abs(r - MID) < w) return true;
        double deg = Math.toDegrees(Math.atan2(z, x)), near = Math.round(deg / 45.0) * 45.0;
        // конец радиального туннеля — по краю города в оси туннеля (а не в текущей точке: край «волнистый»,
        // и торцевая стена местами получалась тоньше блока — бета 12.11: «бездна на конце»)
        double eAxis = edge(Math.toRadians(near));
        if (r > 34 && r < eAxis - 10 + (w > 4 ? 3.0 : 0)) {
            if (r * Math.sin(Math.toRadians(angDiff(deg, near))) < w && Math.cos(Math.toRadians(angDiff(deg, near))) > 0) return true;
        }
        return dist(x, z, RAT_THRONE.getX(), RAT_THRONE.getZ()) < RAT_R + w - 3.5 || dist(x, z, PLUMBER.getX(), PLUMBER.getZ()) < 4 + w - 3.5;
    }

    private void sewer() {
        int f = YS - 1, top = YS + 5;
        for (int x = -R_EDGE; x <= R_EDGE; x++) for (int z = -R_EDGE; z <= R_EDGE; z++) {
            boolean in = tunnel(x, z, 3.5), wall = !in && tunnel(x, z, 5.4);
            if (!in && !wall) continue;
            if (wall) {
                for (int y = f; y <= top; y++) set(x, y, z, (y + x + z) % 5 == 0 ? Blocks.MOSSY_STONE_BRICKS : y % 4 == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS);
                if (Math.floorMod(x * 5 + z * 3, 17) == 0) set(x, YS + 2, z, Blocks.SHROOMLIGHT);   // фонари-грибы в стенах
                continue;
            }
            boolean throne = dist(x, z, RAT_THRONE.getX(), RAT_THRONE.getZ()) < RAT_R;
            double r = Math.hypot(x, z);
            double degXZ = Math.toDegrees(Math.atan2(z, x));
            boolean nearDen = angDiff(degXZ, DEN_DEG) < 12 && r > denEnd() - 24;   // у лавы — без воды (иначе обсидиановый мост)
            boolean channel = !throne && !nearDen && (Math.abs(r - MID) < 1.2 || (Math.abs(r - MID) > 4 && radialW(x, z) < 1.2));
            set(x, f - 1, z, Blocks.STONE_BRICKS);
            set(x, f, z, channel ? Blocks.WATER.defaultBlockState() : rnd.nextFloat() < 0.2f ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState());
            int h = throne ? top + 4 : top;
            for (int y = YS; y < h; y++) set(x, y, z, Blocks.AIR);
            set(x, h, z, (Math.floorMod(x * 7 + z * 13, 11) == 0) ? Blocks.GLOWSTONE : Blocks.STONE_BRICKS);
            if (throne) for (int y = top; y <= h; y++) if (dist(x, z, RAT_THRONE.getX(), RAT_THRONE.getZ()) > RAT_R - 1.2) set(x, y, z, Blocks.MOSSY_STONE_BRICKS);
        }
        // люки: на пересечениях радиальных туннелей и кольцевого стока
        for (int i = 0; i < 8; i++) {
            BlockPos m = polar(i * 45 + 4.5, MID + 2.5, Y);
            for (int y = YS; y < Y; y++) {   // опора лестницы до самого пола канализации (иначе нижние лестницы висели в воздухе и ломались)
                set(m.getX() + 1, y, m.getZ(), Blocks.STONE_BRICKS);
                setLit(m.getX(), y, m.getZ(), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST));
            }
            setLit(m.getX(), Y - 1, m.getZ(), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST));
            set(m.getX() - 1, Y, m.getZ(), Blocks.IRON_BARS); set(m.getX(), Y, m.getZ() + 1, Blocks.IRON_BARS); set(m.getX(), Y, m.getZ() - 1, Blocks.IRON_BARS);
            setLit(m.getX(), YS, m.getZ(), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST));
            set(m.getX() - 1, Y, m.getZ() - 1, Blocks.LANTERN);
        }
        // Тронный сток: островок-трон в центре и лужи
        int cx = RAT_THRONE.getX(), cz = RAT_THRONE.getZ();
        for (int dx = -RAT_R; dx <= RAT_R; dx++) for (int dz = -RAT_R; dz <= RAT_R; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > RAT_R - 1.5) continue;
            if (d > 9 && d < 11.5 && ((dx + dz) & 3) != 0) set(cx + dx, YS - 1, cz + dz, Blocks.WATER);
            if (d < 3) set(cx + dx, YS - 1, cz + dz, Blocks.GOLD_BLOCK);
        }
        set(cx, YS, cz - 4, Blocks.SKELETON_SKULL);
        for (int a = 0; a < 360; a += 45) { BlockPos p = polar(a, 7, 0); set(cx + p.getX(), YS, cz + p.getZ(), Blocks.COBBLESTONE_WALL); setLit(cx + p.getX(), YS + 1, cz + p.getZ(), Blocks.LANTERN.defaultBlockState()); }
        // каморка сантехника
        int px = PLUMBER.getX(), pz = PLUMBER.getZ();
        set(px + 2, YS, pz, Blocks.BARREL); set(px - 2, YS, pz + 1, Blocks.CRAFTING_TABLE); set(px, YS + 3, pz, Blocks.LANTERN);
        sign(new BlockPos(px - 2, YS + 1, pz + 1), Direction.EAST, DyeColor.BLUE, "САНТЕХНИК", "ГОША", "трубы, вентили,", "слухи");
        smugglerDen();
        // тайники канализации
        for (int i = 0; i < 6; i++) {
            BlockPos s = polar(i * 60 + 30, MID - 2.6, YS);
            stash(s.getX(), YS, s.getZ(), SEWER_STASHES);
        }
        stash(cx + 6, YS, cz + 6, SEWER_STASHES);
    }

    /** Паркур над лавой в конце северного туннеля и логово контрабандиста за ним. */
    private void smugglerDen() {
        double end = denEnd();
        double a = Math.toRadians(DEN_DEG);
        double ux = Math.cos(a), uz = Math.sin(a), vx = -uz, vz = ux;   // вдоль туннеля и поперёк
        // лава: от end-19 до end-6 по всей ширине туннеля
        for (double t = end - 19; t <= end - 6; t += 0.5) for (double w = -3; w <= 3; w += 0.5) {
            int x = (int) Math.round(ux * t + vx * w), z = (int) Math.round(uz * t + vz * w);
            set(x, YS - 3, z, Blocks.BLACKSTONE); set(x, YS - 2, z, Blocks.BLACKSTONE); set(x, YS - 1, z, Blocks.LAVA);   // лава на каменном дне — не утекает в бездну
        }
        // ступеньки-островки (прыжки по 2–3 блока, змейкой)
        double[][] steps = {{-17, 0}, {-14.5, 1.5}, {-12, -1}, {-9.5, 1}, {-7.5, -1.5}};
        for (double[] st : steps) {
            int x = (int) Math.round(ux * (end + st[0]) + vx * st[1]), z = (int) Math.round(uz * (end + st[0]) + vz * st[1]);
            set(x, YS - 1, z, Blocks.POLISHED_BLACKSTONE);
            set(x, YS - 2, z, Blocks.BLACKSTONE);
        }
        // логово: ковёр, сундуки, фонари
        for (double t = end - 5; t <= end; t += 0.5) for (double w = -3; w <= 3; w += 0.5) {
            int x = (int) Math.round(ux * t + vx * w), z = (int) Math.round(uz * t + vz * w);
            set(x, YS - 1, z, Blocks.DARK_OAK_PLANKS);
            if (Math.abs(w) <= 1.5 && t < end - 1) set(x, YS, z, Blocks.PURPLE_CARPET);
        }
        BlockPos d = den();
        for (int[] o : new int[][]{{2, 2}, {-2, 2}, {2, -2}, {-2, -2}}) {
            int x = d.getX() + o[0], z = d.getZ() + o[1];
            if (Math.abs(o[0]) == 2 && Math.abs(o[1]) == 2) { set(x, YS, z, Blocks.BARREL); setLit(x, YS + 1, z, Blocks.LANTERN.defaultBlockState()); }
        }
        BlockPos sg = polar(DEN_DEG, end - 20.5, YS);
        sign(new BlockPos(sg.getX(), YS + 1, sg.getZ()), Direction.SOUTH, DyeColor.PURPLE, "ЗА ЛАВОЙ —", "СВОИ.", "Остальные —", "в лаву.");
    }

    private static double radialW(double x, double z) {
        double r = Math.hypot(x, z), deg = Math.toDegrees(Math.atan2(z, x)), near = Math.round(deg / 45.0) * 45.0;
        return r * Math.sin(Math.toRadians(angDiff(deg, near)));
    }

    // ------------------------------------------------------------------ небоскрёбы
    private static final Block[] FACADE = {Blocks.LIGHT_GRAY_CONCRETE, Blocks.GRAY_CONCRETE, Blocks.CYAN_TERRACOTTA, Blocks.WHITE_CONCRETE, Blocks.BLACK_CONCRETE, Blocks.POLISHED_DEEPSLATE, Blocks.SMOOTH_SANDSTONE, Blocks.BRICKS};
    private static final Block[] NEONS = {Blocks.MAGENTA_CONCRETE, Blocks.PINK_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE, Blocks.LIME_CONCRETE, Blocks.PURPLE_CONCRETE};
    private static final Block[] GLASS = {Blocks.LIGHT_BLUE_STAINED_GLASS, Blocks.GRAY_STAINED_GLASS, Blocks.BLACK_STAINED_GLASS, Blocks.CYAN_STAINED_GLASS};

    private boolean lotFree(int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x += Math.max(1, (x1 - x0) / 3)) for (int z = z0; z <= z1; z += Math.max(1, (z1 - z0) / 3))
            if (cell(x, z) != 5) return false;
        return cell(x1, z1) == 5 && cell(x0, z1) == 5 && cell(x1, z0) == 5;
    }

    private void towers() {
        for (int gx = -R_EDGE; gx <= R_EDGE; gx += 13) for (int gz = -R_EDGE; gz <= R_EDGE; gz += 13) {
            int w = 7 + rnd.nextInt(5), d = 7 + rnd.nextInt(5);
            int x0 = gx + rnd.nextInt(3), z0 = gz + rnd.nextInt(3), x1 = x0 + w, z1 = z0 + d;
            if (!lotFree(x0, z0, x1, z1)) continue;
            if (dist((x0 + x1) / 2.0, (z0 + z1) / 2.0, TOWER.getX(), TOWER.getZ()) < 16) continue;
            double r = Math.hypot((x0 + x1) / 2.0, (z0 + z1) / 2.0);
            int h = (int) (r < 70 ? 34 + rnd.nextInt(26) : r < 110 ? 20 + rnd.nextInt(20) : 12 + rnd.nextInt(14));
            tower(x0, z0, x1, z1, h, FACADE[rnd.nextInt(FACADE.length)], GLASS[rnd.nextInt(GLASS.length)], rnd.nextFloat() < 0.6f);
        }
        // «Альтаир»: самый высокий, с лестницей снаружи и площадкой для парашютиста
        int tx = TOWER.getX(), tz = TOWER.getZ();
        tower(tx - 6, tz - 6, tx + 6, tz + 6, TOWER_H, Blocks.WHITE_CONCRETE, Blocks.LIGHT_BLUE_STAINED_GLASS, true);
        for (int y = Y; y <= Y + TOWER_H; y++) {
            set(tx + 7, y, tz, Blocks.WHITE_CONCRETE);
            setLit(tx - 7, y, tz, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST));
        }
        for (int y = Y; y <= Y + TOWER_H; y++) set(tx - 6, y, tz, Blocks.WHITE_CONCRETE);
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) set(tx + dx, Y + TOWER_H, tz + dz, (dx + dz) % 3 == 0 ? Blocks.SEA_LANTERN : Blocks.SMOOTH_QUARTZ);
        set(tx - 6, Y + TOWER_H + 1, tz, Blocks.AIR); set(tx - 6, Y + TOWER_H + 2, tz, Blocks.AIR);   // выход с лестницы на крышу (парапет не мешает)
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) if (Math.abs(dx) == 6 || Math.abs(dz) == 6) if (!(dx == -6 && dz == 0)) set(tx + dx, Y + TOWER_H + 1, tz + dz, Blocks.WHITE_STAINED_GLASS_PANE);
        sign(new BlockPos(tx - 8, Y + 1, tz + 1), Direction.WEST, DyeColor.CYAN, "НЕБОСКРЁБ", "«АЛЬТАИР»", "лестница наверх", "→ парашютист");
    }

    private void tower(int x0, int z0, int x1, int z1, int h, Block wall, Block glass, boolean neon) {
        Block sign = NEONS[rnd.nextInt(NEONS.length)];
        int signFace = rnd.nextInt(4), signY = Y + 4 + rnd.nextInt(Math.max(1, Math.min(10, h - 8)));
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            boolean ex = x == x0 || x == x1, ez = z == z0 || z == z1;
            if (!ex && !ez) { set(x, Y, z, Blocks.SMOOTH_STONE); set(x, Y + h, z, Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP)); continue; }
            boolean corner = ex && ez;
            int face = z == z0 ? 0 : x == x1 ? 1 : z == z1 ? 2 : 3;
            int u = (face == 0 || face == 2) ? x - x0 : z - z0;
            for (int y = Y; y <= Y + h; y++) {
                int ly = y - Y;
                Block b;
                if (corner || ly == 0 || ly % 4 == 0 || u % 3 == 0) b = wall;
                else b = rnd.nextFloat() < 0.08f ? Blocks.SEA_LANTERN : glass;
                if (neon && face == signFace && !corner && y >= signY && y < signY + 3 && u >= 1 && u <= 5) b = (y == signY + 1 && u % 2 == 0) ? Blocks.GLOWSTONE : sign;
                if (ly == 0 && !corner && u == 2 && face == (signFace + 2) % 4) b = Blocks.IRON_BLOCK;   // «дверь»
                set(x, y, z, b);
            }
            set(x, Y + h + 1, z, wall);   // парапет
        }
        if (rnd.nextFloat() < 0.5f) { int cx = (x0 + x1) / 2, cz = (z0 + z1) / 2; set(cx, Y + h + 1, cz, Blocks.IRON_BLOCK); set(cx + 1, Y + h + 1, cz, Blocks.IRON_TRAPDOOR); }
        if (rnd.nextFloat() < 0.3f) { int cx = x0 + 2, cz = z0 + 2; for (int y = 1; y <= 3; y++) set(cx, Y + h + y, cz, y == 3 ? Blocks.RED_CONCRETE : Blocks.IRON_BARS); }
    }

    // ------------------------------------------------------------------ хаб: станция лифта и зал Тимофея
    private void hub() {
        // станция: кабина — 3D-сущность (Monorail.ensure), под ней стеклянная шахта, рельс уходит в бездну и в небо
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) for (int y = Y; y < Y + 7; y++) set(x, y, z, Blocks.AIR);
        for (int t = 0; t < 60; t++) for (int x = -1; x <= 1; x++) set(x, Y + 7 + t / 3, 3 + t, Blocks.AIR);   // старый блок-монорельс 0.8.0
        for (int x = -2; x <= 2; x++) for (int z = -3; z <= 3; z++) { set(x, Y - 1, z, Blocks.GRAY_STAINED_GLASS); set(x, Y - 2, z, Blocks.AIR); }
        for (int a = 0; a < 360; a += 45) { BlockPos q = polar(a, 4.5, Y); set(q.getX(), Y, q.getZ(), Blocks.POLISHED_BLACKSTONE_WALL); setLit(q.getX(), Y + 1, q.getZ(), Blocks.SEA_LANTERN.defaultBlockState()); }
        sign(new BlockPos(-3, Y, 4), Direction.SOUTH, DyeColor.LIGHT_BLUE, "НЕБЕСНЫЙ", "ЛИФТ", "ПКМ по кабине —", "табло этажей");
        // зал Тимофея (север хаба)
        int gx = GYM.getX(), gz = GYM.getZ();
        for (int x = gx - 7; x <= gx + 7; x++) for (int z = gz - 6; z <= gz + 5; z++) {
            boolean e = x == gx - 7 || x == gx + 7 || z == gz - 6 || z == gz + 5;
            set(x, Y - 1, z, Blocks.ORANGE_TERRACOTTA);
            for (int y = Y; y < Y + 5; y++) set(x, y, z, !e ? Blocks.AIR : (z == gz + 5 && Math.abs(x - gx) <= 1 && y < Y + 3) ? Blocks.AIR : y >= Y + 1 && y <= Y + 2 && (x + z) % 3 == 0 ? Blocks.GLASS : Blocks.RED_CONCRETE);
            set(x, Y + 5, z, (x + z) % 4 == 0 ? Blocks.GLOWSTONE : Blocks.WHITE_CONCRETE);
        }
        for (int x : new int[]{gx - 5, gx + 5}) { set(x, Y, gz - 3, Blocks.WHITE_WOOL); set(x, Y + 1, gz - 3, Blocks.WHITE_WOOL); set(x, Y + 2, gz - 3, Blocks.CHAIN); }   // груши
        set(gx - 3, Y, gz - 4, Blocks.ANVIL); set(gx + 3, Y, gz - 4, Blocks.ANVIL);                                                                  // «гири»
        sign(new BlockPos(gx + 2, Y + 2, gz + 6), Direction.SOUTH, DyeColor.RED, "ЗАЛ", "ТИМОФЕЯ", "дневник подвигов", "вехи → бонусы");
        // скамейки и деревья хаба
        for (int a = 0; a < 360; a += 45) {
            BlockPos p = polar(a + 22.5, 18, Y);
            set(p.getX(), Y - 1, p.getZ(), Blocks.GRASS_BLOCK); tree(p.getX(), p.getZ());
        }
    }

    private void tree(int x, int z) {
        for (int y = Y; y < Y + 4; y++) set(x, y, z, Blocks.CHERRY_LOG);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = 3; dy <= 5; dy++)
            if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy - 4) <= 3 && air(x + dx, Y + dy, z + dz)) set(x + dx, Y + dy, z + dz, Blocks.CHERRY_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
    }

    // ------------------------------------------------------------------ площади
    private void plazas() {
        // Неоновая площадь: арка-вывеска и колонны
        int nx = NEON.getX(), nz = NEON.getZ();
        for (int a = 0; a < 360; a += 30) {
            BlockPos p = polar(a, 13, 0);
            for (int y = Y; y < Y + 6; y++) set(nx + p.getX(), y, nz + p.getZ(), y == Y + 5 ? Blocks.SEA_LANTERN : NEONS[(a / 30) % NEONS.length]);
        }
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) set(nx + dx, Y - 1, nz + dz, Blocks.SEA_LANTERN);
        set(nx, Y, nz, Blocks.BEACON);
        stash(nx + 9, Y, nz - 3, STASHES);
        // рынок: прилавки с навесами
        int mx = MARKET.getX(), mz = MARKET.getZ();
        Block[] wool = {Blocks.RED_WOOL, Blocks.YELLOW_WOOL, Blocks.BLUE_WOOL, Blocks.GREEN_WOOL, Blocks.ORANGE_WOOL};
        for (int i = 0; i < 8; i++) {
            BlockPos p = polar(i * 45, 9, 0);
            int sx = mx + p.getX(), sz = mz + p.getZ();
            for (int dx = -1; dx <= 1; dx++) { set(sx + dx, Y, sz, Blocks.BARREL); set(sx + dx, Y + 3, sz, wool[i % wool.length]); set(sx + dx, Y + 3, sz + 1, wool[i % wool.length]); }
            set(sx - 1, Y + 1, sz + 1, Blocks.OAK_FENCE); set(sx - 1, Y + 2, sz + 1, Blocks.OAK_FENCE); set(sx + 1, Y + 1, sz + 1, Blocks.OAK_FENCE); set(sx + 1, Y + 2, sz + 1, Blocks.OAK_FENCE);
        }
        stash(mx, Y, mz, STASHES);
        // парк: трава, деревья, фонтан
        int px = PARK.getX(), pz = PARK.getZ();
        for (int dx = -PLAZA_R; dx <= PLAZA_R; dx++) for (int dz = -PLAZA_R; dz <= PLAZA_R; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > PLAZA_R - 1) continue;
            boolean path = Math.abs(dx) <= 1 || Math.abs(dz) <= 1;
            set(px + dx, Y - 1, pz + dz, path ? Blocks.GRAVEL : Blocks.GRASS_BLOCK);
            if (!path && d > 4 && rnd.nextFloat() < 0.15f) set(px + dx, Y, pz + dz, rnd.nextBoolean() ? Blocks.SHORT_GRASS : Blocks.POPPY);
        }
        for (int a = 20; a < 360; a += 60) { BlockPos p = polar(a, 10, 0); tree(px + p.getX(), pz + p.getZ()); }
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
            if (rim) set(px + dx, Y, pz + dz, Blocks.STONE_BRICK_WALL); else level.setBlock(new BlockPos(px + dx, Y - 1, pz + dz), Blocks.WATER.defaultBlockState(), 3);
        }
        stash(px - 8, Y, pz + 8, STASHES);
        // переулки: тайники у стен
        for (int i = 0; i < 6; i++) { BlockPos s = polar(i * 60 + 10, 100, Y); if (cell(s.getX(), s.getZ()) == 3) stash(s.getX(), Y, s.getZ(), STASHES); }
    }

    // ------------------------------------------------------------------ клуб «Серра» (логово Дона)
    private void club() {
        int cx = CLUB.getX(), cz = CLUB.getZ();
        for (int dx = -CLUB_R - 1; dx <= CLUB_R + 1; dx++) for (int dz = -CLUB_R - 1; dz <= CLUB_R + 1; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > CLUB_R + 0.5) continue;
            set(cx + dx, Y - 1, cz + dz, d < 6 ? (((dx + dz) & 1) == 0 ? Blocks.MAGENTA_CONCRETE : Blocks.BLACK_CONCRETE) : Blocks.POLISHED_BLACKSTONE);
            for (int y = Y; y < Y + 8; y++) set(cx + dx, y, cz + dz, Blocks.AIR);
            if (d > CLUB_R - 0.8) {
                double a = Math.toDegrees(Math.atan2(dz, dx));
                boolean gate = angDiff(a, 135) < 12;   // вход со стороны центра города
                if (!gate) for (int y = Y; y < Y + 6; y++) set(cx + dx, y, cz + dz, y == Y + 4 ? Blocks.MAGENTA_CONCRETE : y == Y + 5 ? Blocks.GLOWSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS);
            }
        }
        for (int a = 0; a < 360; a += 60) { BlockPos p = polar(a, 9, 0); set(cx + p.getX(), Y, cz + p.getZ(), Blocks.GOLD_BLOCK); set(cx + p.getX(), Y + 1, cz + p.getZ(), Blocks.LANTERN); }
        BlockPos sg = polar(135, CLUB_R + 3, 0);
        sign(new BlockPos(cx + sg.getX(), Y, cz + sg.getZ()), Direction.NORTH, DyeColor.MAGENTA, "КЛУБ «СЕРРА»", "Дон Витторио", "раз в 25 минут", "! PvP-зона !");
    }

    // ------------------------------------------------------------------ фонари, машины
    private void props() {
        Block[] cars = {Blocks.RED_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.WHITE_CONCRETE, Blocks.YELLOW_CONCRETE, Blocks.BLACK_CONCRETE};
        for (int x = -R_EDGE; x <= R_EDGE; x += 3) for (int z = -R_EDGE; z <= R_EDGE; z += 3) {
            int c = cell(x, z);
            if (c == 3 && ((x * 7 + z * 11) % 13 == 0) && air(x, Y, z)) {
                set(x, Y, z, Blocks.POLISHED_BLACKSTONE_WALL); set(x, Y + 1, z, Blocks.IRON_BARS); set(x, Y + 2, z, Blocks.IRON_BARS); set(x, Y + 3, z, Blocks.SEA_LANTERN);
            } else if (c == 2 && rnd.nextFloat() < 0.02f && radialDist(x, z) > 1.5 && air(x, Y, z)) {
                Block body = cars[rnd.nextInt(cars.length)];
                boolean alongX = Math.abs(x) > Math.abs(z);
                for (int i = -1; i <= 1; i++) {
                    int bx = alongX ? x : x + i, bz = alongX ? z + i : z;
                    if (!air(bx, Y, bz)) continue;
                    set(bx, Y, bz, Math.abs(i) == 1 ? Blocks.BLACK_WOOL : body);
                    if (i == 0) set(bx, Y + 1, bz, Blocks.LIGHT_GRAY_STAINED_GLASS);
                }
            }
        }
    }

    // ------------------------------------------------------------------ NPC
    public static void ensureNpcs(net.minecraft.server.MinecraftServer server) {
        ServerLevel lvl = com.pvpciv.world.CivWorld.f2(server);
        if (!com.pvpciv.world.CivWorld.isF2(lvl)) return;
        ensure(lvl, ModRegistry.TIMOFEY.get(), new Vec3(GYM.getX() + 0.5, Y, GYM.getZ() - 1.5));
        ensure(lvl, ModRegistry.PARACHUTIST.get(), new Vec3(TOWER.getX() + 2.5, Y + TOWER_H + 1, TOWER.getZ() + 0.5));
        ensure(lvl, ModRegistry.PLUMBER.get(), new Vec3(PLUMBER.getX() + 0.5, YS, PLUMBER.getZ() - 1.5));
        BlockPos d = den();
        ensure(lvl, ModRegistry.SMUGGLER.get(), new Vec3(d.getX() + 0.5, YS, d.getZ() + 0.5));
    }

    private static void ensure(ServerLevel lvl, net.minecraft.world.entity.EntityType<com.pvpciv.entity.CityNpcEntity> type, Vec3 at) {
        if (!lvl.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(BlockPos.containing(at)))) return;
        var list = lvl.getEntitiesOfClass(com.pvpciv.entity.CityNpcEntity.class, new net.minecraft.world.phys.AABB(BlockPos.containing(at)).inflate(24), e -> e.getType() == type);
        for (int i = 1; i < list.size(); i++) list.get(i).discard();
        if (!list.isEmpty()) {
            if (list.get(0).position().distanceTo(at) > 3) list.get(0).teleportTo(at.x, at.y, at.z);
            return;
        }
        var e = type.create(lvl);
        if (e == null) return;
        e.moveTo(at.x, at.y, at.z, 180, 0);
        lvl.addFreshEntity(e);
    }
}
