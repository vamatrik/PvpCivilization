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
            set(x, RY - 1, z, (x + z) % 6 == 0 ? Blocks.SEA_LANTERN : Math.abs(x) == 7 ? Blocks.GOLD_BLOCK : (x + z) % 2 == 0 ? Blocks.POLISHED_DEEPSLATE : Blocks.SMOOTH_QUARTZ);
            for (int y = RY; y < RY + H; y++) {
                if (!edge) { set(x, y, z, Blocks.AIR); continue; }
                boolean door = (x == X0 || x == X1) && z >= -28 && z <= -25 && y < RY + 3;
                boolean window = !door && y >= RY + 1 && y <= RY + 4 && (x + z) % 4 != 0;
                boolean neon = y == RY + 5;
                set(x, y, z, door ? Blocks.AIR.defaultBlockState() : neon ? Blocks.GOLD_BLOCK.defaultBlockState()
                        : window ? Blocks.GRAY_STAINED_GLASS.defaultBlockState() : Blocks.GRAY_CONCRETE.defaultBlockState());
            }
            set(x, RY + H, z, edge ? Blocks.GRAY_CONCRETE : (x + z) % 5 == 0 ? Blocks.SEA_LANTERN : Blocks.GRAY_STAINED_GLASS);
        }
        // кабина — 3D-сущность (Monorail.ensure); под ней — платформа, табло внутри кабины
        for (int x = -2; x <= 2; x++) for (int z = -29; z <= -25; z++) set(x, RY - 1, z, (x + z) % 2 == 0 ? Blocks.POLISHED_DEEPSLATE : Blocks.SEA_LANTERN);
        set(PANEL.getX(), PANEL.getY(), PANEL.getZ(), Blocks.AIR);
        sign(new BlockPos(-3, RY, -23), Direction.SOUTH, DyeColor.YELLOW, "НЕБЕСНЫЙ", "ЛИФТ", "ПКМ по кабине —", "табло этажей");
        // старый монорельс из блоков (0.8.0) — убрать
        for (int t = 0; t < 190; t++) {
            int y = RY + 6 + t, z = -27 - (int) Math.round(t * 0.75);
            for (int x = -1; x <= 1; x++) { set(x, y, z, Blocks.AIR); set(x, y + 1, z, Blocks.AIR); }
            if (t % 24 == 12 && y < RY + 60) for (int yy = RY + 6; yy < y; yy++) set(0, yy, z, Blocks.AIR);
        }
        // проём в потолке/стене зала по ходу рельса
        for (double t = 0; t <= 14; t += 0.5) {
            Vec3 c = Monorail.roof(t);
            for (int x = -2; x <= 2; x++) for (int dy = 0; dy <= 3; dy++) for (int dz = -2; dz <= 2; dz++) {
                int bx = (int) Math.floor(c.x) + x, by = (int) Math.floor(c.y) + dy, bz = (int) Math.floor(c.z) + dz;
                if (by >= RY && by <= RY + H && bz >= Z0 - 1 && bz <= Z1) set(bx, by, bz, Blocks.AIR);
            }
        }
        // скамейки и растения
        for (int x : new int[]{-11, -8, 8, 11}) {
            set(x, RY, -30, Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
            set(x, RY, -23, Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        }
        for (int x : new int[]{-13, 13}) for (int z : new int[]{-30, -23}) { set(x, RY, z, Blocks.SMOOTH_QUARTZ); set(x, RY + 1, z, Blocks.POTTED_AZALEA); }
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
