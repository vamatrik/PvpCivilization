package com.pvpciv.world;

import com.pvpciv.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;

/**
 * Крыша общежития: хай-тек зал Небесного лифта на северном крыле, кабина в центре,
 * за ней — монорельс, уходящий в небо. Лестницы в углах галереи продлены до крыши.
 */
public class RoofBuilder extends Builder {
    public static final int RY = DormBuilder.Y0 + DormBuilder.FLOORS * DormBuilder.FH;   // уровень пола крыши (124)
    public static final int X0 = -14, X1 = 14, Z0 = -31, Z1 = -22, H = 7;
    public static final BlockPos PANEL = new BlockPos(2, RY, -24);
    public static final Vec3 ARRIVE = new Vec3(0.5, RY, -23.5);

    private RoofBuilder(ServerLevel level) { super(level, 77); }

    public static void build(ServerLevel level) { new RoofBuilder(level).hall(); }

    private void hall() {
        // пол, стены, стеклянный потолок
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1; z++) {
            boolean edge = x == X0 || x == X1 || z == Z0 || z == Z1;
            set(x, RY - 1, z, (x + z) % 6 == 0 ? Blocks.SEA_LANTERN : Math.abs(x) == 7 ? Blocks.LIGHT_BLUE_STAINED_GLASS : (x + z) % 2 == 0 ? Blocks.WHITE_CONCRETE : Blocks.SMOOTH_QUARTZ);
            for (int y = RY; y < RY + H; y++) {
                if (!edge) { set(x, y, z, Blocks.AIR); continue; }
                boolean door = (x == X0 || x == X1) && z >= -28 && z <= -25 && y < RY + 3;
                boolean window = !door && y >= RY + 1 && y <= RY + 4 && (x + z) % 4 != 0;
                boolean neon = y == RY + 5;
                set(x, y, z, door ? Blocks.AIR.defaultBlockState() : neon ? Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState()
                        : window ? Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState() : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
            }
            set(x, RY + H, z, edge ? Blocks.LIGHT_GRAY_CONCRETE : (x + z) % 5 == 0 ? Blocks.SEA_LANTERN : Blocks.LIGHT_BLUE_STAINED_GLASS);
        }
        // кабина лифта: стальные стойки, стеклянные стенки, проём на юг
        for (int x = -2; x <= 2; x++) for (int z = -29; z <= -25; z++) {
            boolean post = Math.abs(x) == 2 && (z == -29 || z == -25);
            boolean wall = Math.abs(x) == 2 || z == -29;
            for (int y = RY; y < RY + 5; y++) {
                if (post) set(x, y, z, Blocks.IRON_BLOCK);
                else if (wall && !(z == -25)) set(x, y, z, y == RY + 4 ? Blocks.IRON_BLOCK : Blocks.LIGHT_BLUE_STAINED_GLASS);
            }
            set(x, RY - 1, z, Blocks.POLISHED_DEEPSLATE);
            set(x, RY + 5, z, Blocks.IRON_BLOCK);
        }
        for (int x = -1; x <= 1; x++) set(x, RY + 4, -25, Blocks.IRON_BLOCK);
        set(0, RY - 1, -27, Blocks.BEACON);
        set(0, RY - 2, -27, Blocks.IRON_BLOCK);
        // табло
        setLit(PANEL.getX(), PANEL.getY(), PANEL.getZ(), ModRegistry.LIFT_PANEL.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        sign(new BlockPos(-2, RY + 1, -24), Direction.SOUTH, DyeColor.LIGHT_BLUE, "НЕБЕСНЫЙ", "ЛИФТ", "табло — справа", "→ этаж 2");
        // скамейки и растения
        for (int x : new int[]{-11, -8, 8, 11}) {
            set(x, RY, -30, Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
            set(x, RY, -23, Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        }
        for (int x : new int[]{-13, 13}) for (int z : new int[]{-30, -23}) { set(x, RY, z, Blocks.SMOOTH_QUARTZ); set(x, RY + 1, z, Blocks.POTTED_AZALEA); }
        // монорельс: из кабины вверх и на север, в небо
        for (int t = 0; t < 190; t++) {
            int y = RY + 6 + t, z = -27 - (int) Math.round(t * 0.75);
            for (int x = -1; x <= 1; x++) {
                set(x, y, z, x == 0 ? (t % 7 == 0 ? Blocks.SEA_LANTERN : Blocks.LIGHT_BLUE_STAINED_GLASS) : Blocks.POLISHED_BLACKSTONE);
                set(x, y + 1, z, x == 0 ? Blocks.AIR : Blocks.IRON_BARS);
            }
            if (t % 24 == 12 && y < RY + 60) for (int yy = RY + 6; yy < y; yy++) set(0, yy, z, Blocks.IRON_BLOCK);   // опоры у основания
        }
        // лестницы с галереи на крышу (в углах)
        int top = DormBuilder.Y0 + (DormBuilder.FLOORS - 1) * DormBuilder.FH + 2;
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) {
            int x = sx * DormBuilder.GAL_OUT, z = sz * DormBuilder.GAL_OUT;
            Direction facing = sx > 0 ? Direction.WEST : Direction.EAST;
            for (int y = top + 1; y < RY; y++) {
                set(x + sx, y, z, Blocks.LIGHT_GRAY_CONCRETE);
                setLit(x, y, z, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing));
            }
        }
        fixShapes();
    }

    @SuppressWarnings("unused") private static final Half H0 = Half.BOTTOM;
}
