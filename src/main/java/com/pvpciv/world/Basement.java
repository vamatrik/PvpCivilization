package com.pvpciv.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;

/**
 * Подвал общежития — владения Кармиллы. Спуск из атриума (юго-западный угол), тронный зал, гроб,
 * клетка для должников, кровавый фонтан и пустой ритуальный круг (будущий стол инфузии).
 */
public final class Basement extends Builder {
    public static final int Y0 = DormBuilder.Y0;
    public static final int FY = Y0 - 9;                // уровень пола (по нему ходят)
    public static final int X0 = -16, X1 = 10, Z0 = -10, Z1 = 11;
    public static final BlockPos CARMILLA_SPOT = new BlockPos(-4, FY, -7);
    public static final BlockPos CELL_SPOT = new BlockPos(7, FY, -8);
    public static final BlockPos CELL_EXIT = new BlockPos(6, FY, -2);
    public static final BlockPos CELL_DOOR = new BlockPos(6, FY, -4);
    public static final BlockPos RITUAL = new BlockPos(-4, FY, 5);
    public static final BlockPos ENTRANCE_TOP = new BlockPos(-14, Y0, 21);

    private Basement(ServerLevel l) { super(l, 666L); }

    public static boolean contains(Vec3 p) {
        return p.x >= X0 - 1 && p.x < X1 + 2 && p.z >= Z0 - 1 && p.z < Z1 + 2 && p.y >= FY - 2 && p.y < Y0 - 3;
    }

    public static boolean inCell(Vec3 p) {
        return p.x >= 4 && p.x < 10 && p.z >= -10 && p.z < -4 && p.y >= FY - 1 && p.y < FY + 4;
    }

    public static void build(ServerLevel level) {
        Basement b = new Basement(level);
        b.shell();
        b.stairs();
        b.throne();
        b.cell(true);
        b.ritual();
        b.decor();
    }

    private void shell() {
        for (int x = X0 - 1; x <= X1 + 1; x++) for (int z = Z0 - 1; z <= Z1 + 1; z++) {
            boolean wall = x == X0 - 1 || x == X1 + 1 || z == Z0 - 1 || z == Z1 + 1;
            set(x, FY - 1, z, wall ? Blocks.DEEPSLATE_BRICKS : ((x + z) & 1) == 0 ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.POLISHED_BLACKSTONE);
            set(x, FY - 2, z, Blocks.COBBLED_DEEPSLATE);
            for (int y = FY; y <= FY + 4; y++) {
                if (wall) set(x, y, z, y == FY + 2 && (x + z) % 4 == 0 ? Blocks.CHISELED_DEEPSLATE : Blocks.DEEPSLATE_BRICKS);
                else set(x, y, z, Blocks.AIR);
            }
            set(x, FY + 5, z, wall ? Blocks.DEEPSLATE_BRICKS : Blocks.DEEPSLATE_TILES);
        }
        // колонны и арки
        for (int x = X0 + 5; x <= X1 - 2; x += 6) for (int z : new int[]{Z1 - 2}) {
            for (int y = FY; y <= FY + 4; y++) set(x, y, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
            set(x, FY + 4, z + 1, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH).setValue(StairBlock.HALF, Half.TOP));
            set(x, FY + 4, z - 1, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH).setValue(StairBlock.HALF, Half.TOP));
        }
        // ковровая дорожка от лестницы к трону
        for (int z = Z1; z >= CARMILLA_SPOT.getZ(); z--) for (int x = -14; x <= -13; x++) set(x, FY, z, Blocks.RED_CARPET);
        for (int x = -14; x <= CARMILLA_SPOT.getX() + 1; x++) for (int z = CARMILLA_SPOT.getZ() - 1; z <= CARMILLA_SPOT.getZ(); z++) set(x, FY, z, Blocks.RED_CARPET);
        // подвесные фонари и тёплые светильники в потолке
        for (int x = X0 + 1; x <= X1 - 1; x += 5) for (int z = Z0 + 4; z <= Z1 - 2; z += 7)
            setLit(x, FY + 4, z, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        for (int x = X0 + 3; x <= X1 - 1; x += 5) for (int z = Z0 + 1; z <= Z1; z += 5)
            setLit(x, FY + 5, z, Blocks.SHROOMLIGHT.defaultBlockState());
        // невидимый мягкий свет (подвал должен быть мрачным, но не чёрным)
        for (int x = X0 + 2; x <= X1 - 1; x += 4) for (int z = Z0 + 1; z <= Z1; z += 4)
            setLit(x, FY + 3, z, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 11));
    }

    /** Спуск из атриума: узкая траншея-лестница с перилами. */
    private void stairs() {
        for (int k = 0; k <= 9; k++) {
            int z = 20 - k, block = Y0 - 1 - k, feet = Y0 - k;
            int airTop = z >= Z1 + 1 ? Y0 + 1 : FY + 4;
            for (int x = -14; x <= -13; x++) {
                set(x, block, z, k < 9 ? Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH) : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                for (int y = feet; y <= airTop; y++) set(x, y, z, Blocks.AIR);
            }
            if (z <= Z1) continue;
            for (int x : new int[]{-15, -12}) {
                for (int y = block - 1; y <= Y0 - 1; y++) set(x, y, z, Blocks.DEEPSLATE_BRICKS);
                set(x, Y0, z, Blocks.DARK_OAK_FENCE);
                if (k % 3 == 0) setLit(x, Y0 + 1, z, Blocks.SOUL_LANTERN.defaultBlockState());
            }
        }
        for (int x = -14; x <= -13; x++) set(x, Y0, 12, Blocks.DARK_OAK_FENCE);
        for (int x = -15; x <= -12; x++) for (int y = Y0 - 10; y <= Y0 - 1; y++) set(x, y, 21, Blocks.DEEPSLATE_BRICKS);
        for (int x = -14; x <= -13; x++) set(x, Y0, 21, Blocks.AIR);
        // арка над входом
        for (int y = Y0; y <= Y0 + 3; y++) { set(-15, y, 21, Blocks.POLISHED_BLACKSTONE_BRICKS); set(-12, y, 21, Blocks.POLISHED_BLACKSTONE_BRICKS); }
        for (int x = -15; x <= -12; x++) set(x, Y0 + 4, 21, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
        set(-14, Y0 + 3, 21, Blocks.CHAIN);
        sign(new BlockPos(-15, Y0 + 2, 22), Direction.SOUTH, DyeColor.RED, "ПОДВАЛ", "Не входить.", "— Комендант", "(там кто-то есть)");
    }

    private void throne() {
        int x = CARMILLA_SPOT.getX(), z = Z0;
        for (int dx = -2; dx <= 2; dx++) {
            set(x + dx, FY, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
            set(x + dx, FY, z + 1, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
        }
        set(x, FY + 1, z, Blocks.BLACKSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        for (int y = FY + 1; y <= FY + 4; y++) { set(x - 1, y, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL); set(x + 1, y, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL); }
        set(x, FY + 2, z, Blocks.RED_WOOL); set(x, FY + 3, z, Blocks.RED_WOOL); set(x, FY + 4, z, Blocks.CRIMSON_PLANKS);
        setLit(x - 1, FY + 5, z, Blocks.SOUL_LANTERN.defaultBlockState()); setLit(x + 1, FY + 5, z, Blocks.SOUL_LANTERN.defaultBlockState());
        for (int dx : new int[]{-2, 2}) setLit(x + dx, FY + 1, z, Blocks.RED_CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true).setValue(CandleBlock.CANDLES, 3));
        setLit(x, FY + 2, z + 3, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15));
        setLit(x, FY + 4, z + 1, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12));
        // знамёна по бокам
        for (int dx : new int[]{-4, 4}) { set(x + dx, FY + 3, z, Blocks.RED_WOOL); set(x + dx, FY + 2, z, Blocks.RED_WOOL); set(x + dx, FY + 1, z, Blocks.BLACK_WOOL); }
    }

    /** Клетка должников. open=false — дверь из решёток закрыта. */
    private void cell(boolean open) {
        for (int x = 3; x <= X1; x++) for (int y = FY; y <= FY + 3; y++) set(x, y, -4, Blocks.IRON_BARS);
        for (int z = Z0; z <= -4; z++) for (int y = FY; y <= FY + 3; y++) set(3, y, z, Blocks.IRON_BARS);
        for (int x = 4; x <= X1; x++) for (int z = Z0; z <= -5; z++) set(x, FY - 1, z, Blocks.COBBLED_DEEPSLATE);
        set(9, FY, -10, Blocks.HAY_BLOCK);
        set(8, FY, -10, Blocks.CAULDRON);
        set(4, FY + 4, -9, Blocks.CHAIN);
        set(4, FY + 3, -9, Blocks.CHAIN);
        set(9, FY + 4, -6, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        setDoor(level, open);
        sign(new BlockPos(5, FY + 2, -3), Direction.SOUTH, DyeColor.RED, "ДОЛЖНИКИ", "Кто не сдал", "контракт —", "посидит.");
    }

    public static void setDoor(ServerLevel level, boolean open) {
        BlockState s = open ? Blocks.AIR.defaultBlockState() : Blocks.IRON_BARS.defaultBlockState();
        for (int x = CELL_DOOR.getX(); x <= CELL_DOOR.getX() + 1; x++)
            for (int y = CELL_DOOR.getY(); y <= CELL_DOOR.getY() + 1; y++)
                level.setBlock(new BlockPos(x, y, CELL_DOOR.getZ()), s, Block.UPDATE_ALL);
    }

    /** Ритуальный круг: 8 пьедесталов вокруг центра — место будущего стола инфузии. */
    private void ritual() {
        int cx = RITUAL.getX(), cz = RITUAL.getZ();
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            double r = Math.sqrt(dx * dx + dz * dz);
            if (r <= 3.4) set(cx + dx, FY - 1, cz + dz, r < 1.5 ? Blocks.CRYING_OBSIDIAN : r > 2.6 ? Blocks.RED_NETHER_BRICKS : Blocks.POLISHED_BLACKSTONE);
        }
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            int x = cx + (int) Math.round(Math.cos(a) * 3), z = cz + (int) Math.round(Math.sin(a) * 3);
            set(x, FY, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
            setLit(x, FY + 1, z, Blocks.RED_CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true).setValue(CandleBlock.CANDLES, 2));
        }
        set(cx, FY, cz, Blocks.LODESTONE);
        sign(new BlockPos(cx, FY, cz + 4), Direction.SOUTH, DyeColor.PURPLE, "Круг Инфузии", "8 звёзд вокруг,", "в центре — дар.", "(скоро)");
    }

    private void decor() {
        // гроб
        for (int z = -9; z <= -7; z++) set(-13, FY, z, Blocks.BLACK_CONCRETE);
        set(-13, FY + 1, -9, Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.BOTTOM));
        set(-13, FY + 1, -8, Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.BOTTOM));
        set(-13, FY + 1, -7, Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.HALF, Half.BOTTOM));
        setLit(-12, FY, -10, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true).setValue(CandleBlock.CANDLES, 3));
        // книжные полки и столик со свечами
        for (int z = -4; z <= 4; z++) for (int y = FY; y <= FY + 2; y++) set(X0, y, z, (z + y) % 3 == 0 ? Blocks.CHISELED_BOOKSHELF : Blocks.BOOKSHELF);
        set(-10, FY, 2, Blocks.DARK_OAK_FENCE); set(-10, FY + 1, 2, Blocks.DARK_OAK_PRESSURE_PLATE);
        setLit(-10, FY + 1, 3, Blocks.AIR.defaultBlockState());
        set(-10, FY, 3, Blocks.DARK_OAK_FENCE);
        setLit(-10, FY + 1, 3, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true).setValue(CandleBlock.CANDLES, 4));
        // кровавый фонтан
        int fx = 6, fz = 6;
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
            set(fx + dx, FY, fz + dz, rim ? Blocks.POLISHED_BLACKSTONE_BRICK_WALL : Blocks.RED_STAINED_GLASS);
            if (!rim) set(fx + dx, FY - 1, fz + dz, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true));
        }
        set(fx, FY - 2, fz, Blocks.REDSTONE_BLOCK);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (dx != 0 || dz != 0) set(fx + dx, FY - 2, fz + dz, Blocks.REDSTONE_BLOCK);
        for (int y = FY + 1; y <= FY + 2; y++) set(fx, y, fz, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
        set(fx, FY + 3, fz, Blocks.CRIMSON_FUNGUS);
        // паутина по углам
        for (int[] c : new int[][]{{X0, Z0}, {X1, Z1}, {X0, Z1}, {2, Z0}}) set(c[0], FY + 4, c[1], Blocks.COBWEB);
        set(-1, FY + 4, 8, Blocks.COBWEB);
    }
}
