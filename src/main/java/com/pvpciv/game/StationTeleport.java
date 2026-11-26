package com.pvpciv.game;

import com.pvpciv.block.ConsoleBlock;
import com.pvpciv.data.StoryData;
import com.pvpciv.floor.Floor5;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.CivWorld;
import com.pvpciv.world.Signs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.1: телепорты станции «Орбита». Площадки — в атриуме, ангаре, на мостике и в особых модулях.
 * ПКМ по пульту площадки — запоминает её; выбор назначения — из запомненных. Бесплатно.
 */
public final class StationTeleport {
    private StationTeleport() {}
    public record Pad(String name, BlockPos console, Direction face) {
        public Vec3 arrive() { BlockPos f = console.relative(face, 2); return new Vec3(f.getX() + 0.5, f.getY(), f.getZ() + 0.5); }
    }

    public static List<Pad> pads() {
        List<Pad> out = new ArrayList<>();
        for (Floor5.Module m : Floor5.MODULES) {
            BlockPos c = m.center();
            switch (m.type()) {
                case ATRIUM -> out.add(new Pad("Атриум", c.offset(-7, 0, 10), Direction.NORTH));
                case HANGAR -> out.add(new Pad("Ангар", c.offset(-11, 0, -10), Direction.EAST));
                case BRIDGE -> out.add(new Pad("Мостик", c.offset(-12, 0, 12), Direction.EAST));
                case OBSERVATORY, ENGINE, MEDBAY -> out.add(new Pad(m.type().title + " " + (char) ('A' + (m.gx() + m.gz()) % 26), c.offset(12, 0, 12), Direction.WEST));
                default -> {}
            }
        }
        return out;
    }

    public static void build(ServerLevel l) {
        for (Pad p : pads()) {
            BlockPos k = p.console;
            BlockPos f = k.relative(p.face, 2);
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                BlockPos q = f.offset(x, 0, z);
                for (int y = 0; y < 3; y++) l.setBlock(q.above(y), Blocks.AIR.defaultBlockState(), 2);
                l.setBlock(q.below(), x == 0 && z == 0 ? Blocks.BEACON.defaultBlockState() : Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), 2);
                l.setBlock(q.below(2), Blocks.SEA_LANTERN.defaultBlockState(), 2);
            }
            for (int y = 0; y < 3; y++) l.setBlock(k.above(y), Blocks.AIR.defaultBlockState(), 2);
            l.setBlock(k.relative(p.face), Blocks.AIR.defaultBlockState(), 2);
            l.setBlock(k, ConsoleBlock.of(ConsoleBlock.Kind.TELEPORT, p.face), Block.UPDATE_ALL);
            Signs.place(l, k.above(1), p.face, 5, true, 0x9EE7FF, "ТЕЛЕПОРТ", p.name, "ПКМ по пульту", "");
        }
    }

    private static int idx(BlockPos pos) { List<Pad> ps = pads(); for (int i = 0; i < ps.size(); i++) if (ps.get(i).console.equals(pos)) return i; return -1; }

    /** 1.2: площадки «Орбиты» убраны — на станции одна система (врата). Пульт и табличка снимаются, маяк гаснет. */
    public static void decommission(ServerLevel l) {
        if (l == null) return;
        for (Pad p : pads()) {
            BlockPos k = p.console, f = k.relative(p.face, 2);
            l.setBlock(k, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            l.setBlock(k.above(1), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            l.setBlock(f.below(), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), 2);
        }
    }

    public static boolean onUse(ServerPlayer p, BlockPos pos) {
        if (true) return false;   // 1.2: отключено (см. decommission)
        if (!CivWorld.isF5(p.level())) return false;
        int i = idx(pos);
        if (i < 0) return false;
        List<Pad> ps = pads();
        StoryData d = Progress.d(p);
        int known = d.gates.getInt("tp5");
        if ((known & (1 << i)) == 0) {
            known |= 1 << i; d.gates.putInt("tp5", known); p.setData(ModRegistry.STORY.get(), d);
            p.displayClientMessage(Component.literal("Площадка «" + ps.get(i).name + "» запомнена.").withStyle(ChatFormatting.AQUA), false);
        }
        DialogueManager.Dialogue dl = new DialogueManager.Dialogue("a");
        List<DialogueManager.Opt> opts = new ArrayList<>();
        for (int j = 0; j < ps.size(); j++) if (j != i && (known & (1 << j)) != 0) { opts.add(DialogueManager.opt("→ " + ps.get(j).name, "!t" + j)); final int jj = j; dl.action("t" + j, pp -> go(pp, ps.get(jj))); }
        opts.add(DialogueManager.opt("[Отмена]", null));
        dl.node("a", "Телепорт «Орбита»", 0x9EE7FF, opts.size() == 1 ? "Других площадок ты ещё не знаешь. Найди их на станции и коснись пульта." : "Куда отправить?", opts.toArray(new DialogueManager.Opt[0]));
        DialogueManager.open(p, null, dl);
        return true;
    }

    private static void go(ServerPlayer p, Pad to) {
        ServerLevel l = p.serverLevel();
        l.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
        l.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1.2f);
        Vec3 a = to.arrive();
        float yaw = to.face.getOpposite().toYRot();
        p.teleportTo(l, a.x, a.y, a.z, yaw, 0);
        p.resetFallDistance();
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(22, "{\"cut\":\"arrive\",\"len\":16}"));
        l.sendParticles(ParticleTypes.END_ROD, a.x, a.y + 1, a.z, 30, 0.4, 0.8, 0.4, 0.05);
        l.playSound(null, BlockPos.containing(a), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1.4f);
    }

    /** Ванильные блоки на месте пультов в уже построенном мире — заменить. */
    public static void convertConsoles(net.minecraft.server.MinecraftServer s) {
        ServerLevel f5 = CivWorld.f5(s);
        for (BlockPos k : new BlockPos[]{Floor5.LIFT_CONSOLE(), Floor5.AIRLOCK(), Floor5.HANGAR_CONSOLE(), com.pvpciv.floor.Belt.CONSOLE()})
            f5.setBlock(k, ConsoleBlock.of(ConsoleBlock.Kind.STATION, Direction.NORTH), Block.UPDATE_ALL);
        CivWorld.f4(s).setBlock(com.pvpciv.floor.Floor4.ENCH_TABLE, ConsoleBlock.of(ConsoleBlock.Kind.LAB, Direction.SOUTH), Block.UPDATE_ALL);
        CivWorld.level(s).setBlock(com.pvpciv.world.DormBuilder.LUNA_DIARY, ConsoleBlock.of(ConsoleBlock.Kind.STAND, Direction.WEST), Block.UPDATE_ALL);
        for (Library.Tome t : Library.TOMES) {
            ServerLevel l = CivWorld.byFloor(s, t.floor());
            BlockPos q = Library.where(s, t);
            if (l.getBlockState(q).is(Blocks.LECTERN)) l.setBlock(q, ConsoleBlock.of(ConsoleBlock.Kind.STAND, t.floor() == 3 ? Direction.EAST : Direction.SOUTH), Block.UPDATE_ALL);
        }
    }
}
