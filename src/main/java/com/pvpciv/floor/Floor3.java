package com.pvpciv.floor;

import com.pvpciv.world.Builder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Этаж 3 мира 1 — «Тихая библиотека» (0.9.0). Отдельное измерение pvpciv:world1_f3.
 * В центре летающего острова — круглая двухэтажная японская библиотека размером с общежитие:
 *  • 1 этаж: атриум с лифтом в центре, стойка библиотекаря, радиальные ряды стеллажей;
 *  • 2 этаж: галерея вокруг атриума, комната убийцы (татами, восток);
 *  • крыша — ярусная, тёмная черепица с красной кромкой, стеклянный купол над атриумом;
 *  • 4 входа со сторон света, у каждого — красные тории.
 */
public final class Floor3 extends Builder {
    public static final int Y = 100;                    // пол 1 этажа
    public static final int R = 30, ATR = 9, FL2 = Y + 7, ROOF = Y + 15, ISLAND = 52;
    public static final Vec3 LIFT_ARRIVE = new Vec3(0.5, Y, 4.5);
    public static final Vec3 LIBRARIAN = new Vec3(0.5, Y, 15.5);   // за стойкой, лицом к атриуму
    public static final Vec3 ASSASSIN = new Vec3(18.5, FL2 + 1, 0.5);
    /** Тайник 3-й утерянной книги — на 2 этаже, за стеллажами на западе. */
    public static final BlockPos BOOK3 = new BlockPos(-24, FL2 + 1, 3);
    public static final List<BlockPos> STASHES = new ArrayList<>();

    private Floor3(ServerLevel level) { super(level, 303); }

    public static void build(ServerLevel level) {
        Floor3 b = new Floor3(level);
        STASHES.clear();
        b.island();
        b.library();
        b.roof();
        b.outside();
        b.fixShapes();
    }

    // ------------------------------------------------------------------ зоны
    public static final Zones.Zone LIBRARY = new Zones.Zone("Этаж 3 · Тихая библиотека", Zones.Type.SAFE);
    public static final Zones.Zone GARDEN = new Zones.Zone("Этаж 3 · Сад библиотеки", Zones.Type.SAFE);
    public static final Zones.Zone SKY = new Zones.Zone("Небо над Шпилем", Zones.Type.SAFE);

    public static Zones.Zone zone(Vec3 p) {
        double r = Math.hypot(p.x, p.z);
        if (r <= R + 1 && p.y >= Y - 2 && p.y < ROOF + 14) return LIBRARY;
        if (r <= ISLAND + 2 && p.y >= Y - 12) return GARDEN;
        return Floor3Areas.zone(p, SKY);
    }

    // ------------------------------------------------------------------ остров
    private void island() {
        for (int x = -ISLAND - 2; x <= ISLAND + 2; x++) for (int z = -ISLAND - 2; z <= ISLAND + 2; z++) {
            double r = Math.hypot(x, z), edge = ISLAND + 1.5 * Math.sin(Math.atan2(z, x) * 7) + Math.cos(Math.atan2(z, x) * 3);
            if (r > edge) continue;
            int depth = 4 + (int) Math.round((edge - r) * 0.35 + rnd.nextInt(2));
            for (int d = 1; d <= depth; d++) set(x, Y - d, z, d == 1 ? (r < R + 1 ? Blocks.STONE_BRICKS : Blocks.GRASS_BLOCK) : d < 3 ? Blocks.DIRT : pick(Blocks.STONE, Blocks.STONE, Blocks.ANDESITE, Blocks.TUFF));
            for (int y = Y; y <= Y + 30; y++) set(x, y, z, Blocks.AIR);
        }
        // каменные дорожки к четырём входам и кольцо вокруг здания
        for (int x = -ISLAND; x <= ISLAND; x++) for (int z = -ISLAND; z <= ISLAND; z++) {
            double r = Math.hypot(x, z);
            boolean ring = r > R + 1 && r < R + 4;
            boolean path = r > R && r < ISLAND - 2 && (Math.abs(x) <= 1 || Math.abs(z) <= 1);
            if ((ring || path) && level.getBlockState(new BlockPos(x, Y - 1, z)).is(Blocks.GRASS_BLOCK))
                set(x, Y - 1, z, (x * 7 + z * 3) % 5 == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.POLISHED_ANDESITE);
        }
    }

    /** 0.9.7: вернуть верхнюю ступень лестницы на 2 этаж (её съедал проём). */
    public static void fixStairs(ServerLevel level) {
        for (int x = -16; x <= -14; x++) level.setBlock(new BlockPos(x, FL2, 17), Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), 3);
    }

    // ------------------------------------------------------------------ здание
    private boolean door(int x, int z) { return (Math.abs(x) <= 1 && Math.abs(z) > R - 3) || (Math.abs(z) <= 1 && Math.abs(x) > R - 3); }

    private void library() {
        for (int x = -R - 1; x <= R + 1; x++) for (int z = -R - 1; z <= R + 1; z++) {
            double r = Math.hypot(x, z);
            if (r > R + 0.5) continue;
            // пол 1 этажа: татами в клетку, у атриума — светлый камень
            Block f1 = r <= ATR ? ((x + z) % 2 == 0 ? Blocks.POLISHED_DIORITE : Blocks.SMOOTH_STONE)
                    : (Math.floorMod(x, 4) < 2) ^ (Math.floorMod(z, 4) < 2) ? Blocks.BAMBOO_MOSAIC : Blocks.BIRCH_PLANKS;
            set(x, Y - 1, z, f1);
            // пол 2 этажа — галерея вокруг атриума
            if (r > ATR + 0.5) set(x, FL2, z, (x + z) % 7 == 0 ? Blocks.STRIPPED_DARK_OAK_WOOD : Blocks.DARK_OAK_PLANKS);
            // стены
            if (r > R - 0.5) for (int y = Y; y < ROOF; y++) set(x, y, z, wall(x, y, z));
        }
        // проходы-двери 3×4 на обоих этажах не нужны наверху; внизу — 4 входа
        for (int x = -R - 1; x <= R + 1; x++) for (int z = -R - 1; z <= R + 1; z++)
            if (Math.hypot(x, z) > R - 2.5 && Math.hypot(x, z) <= R + 0.5 && door(x, z)) for (int y = Y; y < Y + 4; y++) set(x, y, z, Blocks.AIR);
        // перила галереи вокруг атриума
        for (int x = -ATR - 1; x <= ATR + 1; x++) for (int z = -ATR - 1; z <= ATR + 1; z++) {
            double r = Math.hypot(x, z);
            if (r > ATR + 0.5 && r <= ATR + 1.5) set(x, FL2 + 1, z, Blocks.DARK_OAK_FENCE);
        }
        // колонны атриума с красными фонарями
        for (int a = 0; a < 360; a += 45) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * (ATR + 1)), z = (int) Math.round(Math.sin(Math.toRadians(a)) * (ATR + 1));
            for (int y = Y; y < ROOF; y++) set(x, y, z, y == FL2 ? Blocks.RED_TERRACOTTA : Blocks.STRIPPED_CHERRY_LOG);
            setLit(x, FL2 - 1, z + (z > 0 ? -1 : z < 0 ? 1 : 0), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        stacks();
        lift();
        stairs();
        desk();
        assassinRoom();
        // свет: фонари под потолком обоих этажей
        for (int a = 0; a < 360; a += 20) for (int rr : new int[]{16, 24}) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a + rr)) * rr), z = (int) Math.round(Math.sin(Math.toRadians(a + rr)) * rr);
            if (air(x, FL2 - 1, z)) setLit(x, FL2 - 1, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            if (air(x, ROOF - 1, z)) setLit(x, ROOF - 1, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
    }

    private BlockState wall(int x, int y, int z) {
        double ang = Math.toDegrees(Math.atan2(z, x));
        boolean pillar = Math.abs(ang % 15) < 2.2 || Math.abs(ang % 15) > 12.8;
        int ly = (y - Y) % 8;   // 0..7 внутри этажа
        if (pillar) return Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
        if (y == FL2) return Blocks.RED_TERRACOTTA.defaultBlockState();
        if (ly == 0) return Blocks.DARK_OAK_PLANKS.defaultBlockState();
        if (ly == 6) return Blocks.DARK_OAK_PLANKS.defaultBlockState();
        if (ly >= 2 && ly <= 4) return ((int) Math.round(ang / 3.0)) % 3 == 0 ? Blocks.SPRUCE_PLANKS.defaultBlockState() : Blocks.WHITE_STAINED_GLASS.defaultBlockState();   // сёдзи
        return Blocks.WHITE_CONCRETE.defaultBlockState();
    }

    /** Радиальные ряды стеллажей: на 1 этаже 12 рядов, на 2 — 8, вдоль стен — кольцо полок. */
    private void stacks() {
        for (int floor = 0; floor < 2; floor++) {
            int base = floor == 0 ? Y : FL2 + 1, h = floor == 0 ? 5 : 4, n = floor == 0 ? 12 : 8;
            for (int i = 0; i < n; i++) {
                double a = Math.toRadians(360.0 * i / n + 15);
                if (floor == 1 && Math.cos(a) > 0.75) continue;   // восток 2 этажа — комната убийцы
                for (double d = ATR + 4; d <= R - 4; d += 0.5) {
                    if (d > 17.5 && d < 19.5) continue;            // поперечный проход
                    for (int w = -1; w <= 0; w++) {
                        int x = (int) Math.round(Math.cos(a) * d - Math.sin(a) * w), z = (int) Math.round(Math.sin(a) * d + Math.cos(a) * w);
                        if (door(x, z) || Math.hypot(x, z) > R - 2) continue;
                        for (int y = base; y < base + h; y++) set(x, y, z, y == base + h - 1 ? Blocks.DARK_OAK_SLAB : (y + x + z) % 9 == 0 ? Blocks.CHISELED_BOOKSHELF : Blocks.BOOKSHELF);
                    }
                }
            }
            // полки вдоль стены
            for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
                double r = Math.hypot(x, z);
                if (r > R - 1.5 && r <= R - 0.5 && !door(x, z) && !(floor == 1 && x > 12))
                    for (int y = base; y < base + 3; y++) set(x, y, z, Blocks.BOOKSHELF);
            }
        }
    }

    /** Станция лифта: круглая платформа, стеклянный колодец, из которого поднимается кабина. */
    private void lift() {
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
            double r = Math.hypot(x, z);
            if (r > 4.5) continue;
            set(x, Y - 1, z, r < 2 ? Blocks.LIGHT_BLUE_STAINED_GLASS : r < 3.2 ? Blocks.SEA_LANTERN : Blocks.POLISHED_DEEPSLATE);
        }
        signPost(3, Y, 6, Direction.SOUTH, DyeColor.RED, Blocks.DARK_OAK_FENCE, "НЕБЕСНЫЙ", "ЛИФТ", "ПКМ по кабине —", "табло этажей");
    }

    /** Лестница на 2 этаж — по прямой на юго-западе, вдоль стены атриума. */
    private void stairs() {
        for (int i = 0; i < 8; i++) {
            int z = 10 + i;
            for (int x = -16; x <= -14; x++) {
                set(x, Y + i, z, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
                for (int y = Y + i + 1; y <= Y + i + 3; y++) set(x, y, z, Blocks.AIR);
                if (i < 7) set(x, Y + i, z + 1, Blocks.AIR);
            }
            set(-17, Y + i + 1, z, Blocks.DARK_OAK_FENCE);
            set(-13, Y + i + 1, z, Blocks.DARK_OAK_FENCE);
        }
        for (int x = -16; x <= -14; x++) for (int z = 10; z <= 18; z++) if (z < 17) set(x, FL2, z, Blocks.AIR);   // бета 0.9.6: верхняя ступень (z=17) остаётся
        for (int x = -16; x <= -14; x++) for (int z = 10; z <= 17; z++) for (int y = FL2 + 1; y <= FL2 + 3; y++) if (y - Y > z - 10 + 1) set(x, y, z, Blocks.AIR);
        signPost(-12, Y, 9, Direction.SOUTH, DyeColor.WHITE, Blocks.DARK_OAK_FENCE, "2 ЭТАЖ", "архив и", "чайная комната", "");
    }

    /** Стойка библиотекаря — полукругом к югу от атриума. */
    private void desk() {
        for (int x = -3; x <= 3; x++) {
            set(x, Y, 14, Blocks.STRIPPED_DARK_OAK_LOG);
            set(x, Y + 1, 14, x == 0 ? Blocks.LECTERN : Blocks.DARK_OAK_SLAB);
        }
        setLit(-3, Y + 2, 14, Blocks.LANTERN.defaultBlockState());
        setLit(3, Y + 2, 14, Blocks.LANTERN.defaultBlockState());
        for (int x = -2; x <= 2; x++) for (int z = 15; z <= 16; z++) for (int y = Y; y < Y + 4; y++) set(x, y, z, Blocks.AIR);
        for (int x = -2; x <= 2; x++) for (int z = 15; z <= 16; z++) set(x, Y - 1, z, Blocks.STRIPPED_BIRCH_WOOD);
        set(-3, Y, 15, Blocks.BOOKSHELF); set(3, Y, 15, Blocks.BOOKSHELF);
        signPost(-5, Y, 10, Direction.SOUTH, DyeColor.WHITE, Blocks.DARK_OAK_FENCE, "БИБЛИОТЕКАРЬ", "Хранитель", "свитков", "");
        // третья книга — в нише за полками
        set(BOOK3.getX(), BOOK3.getY() - 1, BOOK3.getZ(), Blocks.DARK_OAK_PLANKS);
        set(BOOK3.getX(), BOOK3.getY(), BOOK3.getZ(), Blocks.LECTERN);
        for (int y = 0; y < 3; y++) set(BOOK3.getX() + 1, BOOK3.getY() + y, BOOK3.getZ(), Blocks.AIR);
    }

    /** Чайная комната убийцы: восток 2 этажа, татами, низкий столик, стойка с катанами. */
    private void assassinRoom() {
        for (int x = 12; x <= R; x++) for (int z = -10; z <= 10; z++) {
            double r = Math.hypot(x, z);
            if (r > R - 1.5) continue;
            set(x, FL2, z, (Math.floorMod(x, 3) == 0 || Math.floorMod(z, 4) == 0) ? Blocks.STRIPPED_BAMBOO_BLOCK : Blocks.BAMBOO_MOSAIC);
            for (int y = FL2 + 1; y < ROOF; y++) if (r < R - 1.5) set(x, y, z, Blocks.AIR);
            if (x == 12 && Math.abs(z) > 2) for (int y = FL2 + 1; y < FL2 + 4; y++) set(x, y, z, y == FL2 + 3 ? Blocks.DARK_OAK_PLANKS : Blocks.WHITE_STAINED_GLASS_PANE);
        }
        // столик и подушки
        for (int z = -1; z <= 1; z++) set(22, FL2 + 1, z, Blocks.DARK_OAK_SLAB);
        set(21, FL2 + 1, -1, Blocks.RED_CARPET); set(21, FL2 + 1, 1, Blocks.RED_CARPET); set(23, FL2 + 1, 0, Blocks.RED_CARPET);
        // стойка с катанами (рамки на стене — просто тёмные полосы) и красные фонари
        for (int z = -4; z <= 4; z++) set(R - 2, FL2 + 2, z, Math.abs(z) % 2 == 0 ? Blocks.BLACKSTONE_SLAB : Blocks.DARK_OAK_TRAPDOOR);
        for (int[] l : new int[][]{{15, -7}, {15, 7}, {26, -7}, {26, 7}}) setLit(l[0], FL2 + 1, l[1], Blocks.SOUL_LANTERN.defaultBlockState());
        set(13, FL2 + 1, 0, Blocks.AIR); set(13, FL2 + 2, 0, Blocks.AIR);
    }

    // ------------------------------------------------------------------ крыша
    private void roof() {
        for (int x = -R - 4; x <= R + 4; x++) for (int z = -R - 4; z <= R + 4; z++) {
            double r = Math.hypot(x, z);
            if (r > R + 3.5) continue;
            if (r <= ATR - 1) { set(x, ROOF + 6, z, (x + z) % 3 == 0 ? Blocks.SEA_LANTERN : Blocks.LIGHT_BLUE_STAINED_GLASS); continue; }   // купол над атриумом
            // ярус: от карниза (R+3, ROOF) вверх к атриуму, с загнутыми краями
            int y = ROOF + (int) Math.floor(Math.max(0, R + 3 - r) * 0.28);
            if (r > R + 2.2) y = ROOF;          // карниз чуть приподнят
            boolean eave = r > R + 2.2;
            set(x, y, z, eave ? Blocks.RED_TERRACOTTA : Blocks.DEEPSLATE_TILES);
            if (!eave && r > R - 0.5) set(x, y - 1, z, Blocks.DARK_OAK_PLANKS);
            if (r > ATR - 1 && r < ATR + 0.5) for (int yy = y; yy <= ROOF + 6; yy++) set(x, yy, z, Blocks.DEEPSLATE_TILES);
        }
        // загнутые углы карниза — 8 «рогов»
        for (int a = 0; a < 360; a += 45) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * (R + 4)), z = (int) Math.round(Math.sin(Math.toRadians(a)) * (R + 4));
            set(x, ROOF + 1, z, Blocks.RED_TERRACOTTA);
            set(x, ROOF + 2, z, Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.HALF, Half.BOTTOM));
            setLit(x, ROOF, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        }
        // шпиль
        for (int y = ROOF + 7; y <= ROOF + 13; y++) set(0, y, 0, y == ROOF + 13 ? Blocks.GOLD_BLOCK : Blocks.STRIPPED_CHERRY_LOG);
    }

    // ------------------------------------------------------------------ сад
    private void outside() {
        // тории у входов
        for (int[] d : new int[][]{{0, 1}, {0, -1}, {1, 0}, {-1, 0}}) {
            int cx = d[0] * (R + 9), cz = d[1] * (R + 9);
            int px = d[1] != 0 ? 1 : 0, pz = d[0] != 0 ? 1 : 0;   // поперёк дороги
            for (int s = -1; s <= 1; s += 2) for (int y = Y; y < Y + 6; y++) set(cx + px * 3 * s, y, cz + pz * 3 * s, Blocks.RED_CONCRETE);
            for (int k = -4; k <= 4; k++) { set(cx + px * k, Y + 6, cz + pz * k, Blocks.BLACK_CONCRETE); if (Math.abs(k) <= 3) set(cx + px * k, Y + 4, cz + pz * k, Blocks.RED_CONCRETE); }
        }
        // сакура и каменные фонари
        for (int i = 0; i < 26; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = R + 7 + rnd.nextDouble() * (ISLAND - R - 12);
            int x = (int) Math.round(Math.cos(a) * r), z = (int) Math.round(Math.sin(a) * r);
            if (Math.abs(x) <= 3 || Math.abs(z) <= 3) continue;
            if (!level.getBlockState(new BlockPos(x, Y - 1, z)).is(Blocks.GRASS_BLOCK)) continue;
            if (i % 3 == 0) { set(x, Y, z, Blocks.STONE_BRICK_WALL); set(x, Y + 1, z, Blocks.POLISHED_ANDESITE); setLit(x, Y + 2, z, Blocks.LANTERN.defaultBlockState()); continue; }
            int h = 4 + rnd.nextInt(2);
            for (int y = Y; y < Y + h; y++) set(x, y, z, Blocks.CHERRY_LOG);
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = -1; dy <= 1; dy++)
                if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy) <= 3 && air(x + dx, Y + h + dy, z + dz)) set(x + dx, Y + h + dy, z + dz, Blocks.CHERRY_LEAVES);
        }
        // пруд с карпами — юго-восток
        for (int x = 34; x <= 44; x++) for (int z = 14; z <= 24; z++) {
            double d = Math.hypot(x - 39, (z - 19) * 1.2);
            if (d < 4.6) { set(x, Y - 1, z, Blocks.WATER); set(x, Y - 2, z, Blocks.WATER); set(x, Y - 3, z, Blocks.MUD); }
            else if (d < 5.6) set(x, Y - 1, z, Blocks.MOSSY_COBBLESTONE);
        }
        Decor3.scatter(level, rnd, 0, Y, 0, ISLAND, false);
        Floor3Areas.build(level, rnd);
    }

    @SuppressWarnings("unused") private static final Block UNUSED = Blocks.AIR;
}
