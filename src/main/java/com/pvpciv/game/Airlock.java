package com.pvpciv.game;

import com.pvpciv.floor.Floor5;
import com.pvpciv.world.CivWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 1.1.1: шлюз ангара — свои створки (блок-дисплеи с полосами), анимация: тревога → створки разъезжаются →
 * воздух вырывается наружу и выносит игрока в космос (камера вперёд), затем — площадка пояса. Створки закрываются сами.
 * Пока закрыто, проём держат невидимые барьеры.
 */
public final class Airlock {
    private Airlock() {}
    public static final String TAG = "pvpciv_airlock";
    private static final int OPEN = 34, FLY = 95, CLOSE_AT = 140;
    private static int t = -1;
    private static final Map<UUID, Entity> SEATS = new HashMap<>();
    private static final List<Entity> LEAVES = new ArrayList<>();
    private static final List<Vec3> BASE = new ArrayList<>();
    private static final List<Integer> SIDE = new ArrayList<>();

    /** Середина проёма (низ). Проём: x −3..3, y 0..4, z = ROOM. */
    private static BlockPos gate() { return Floor5.HANGAR_M.center().offset(0, 0, Floor5.ROOM); }
    public static boolean busy() { return t >= 0; }
    public static boolean riding(ServerPlayer p) { return SEATS.containsKey(p.getUUID()); }

    /** Створки на месте и барьеры в проёме (вызывать при стройке/патче и раз в несколько секунд). */
    public static void ensure(MinecraftServer s) {
        ServerLevel l = CivWorld.f5(s);
        if (l == null || Floor5.HANGAR_M == null) return;
        BlockPos g = gate();
        if (!l.isLoaded(g) || !l.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(g))) return;
        if (t < 0) setGate(l, true);
        List<Entity> have = l.getEntitiesOfClass(Entity.class, new AABB(g).inflate(8), e -> e.getTags().contains(TAG));
        if (!LEAVES.isEmpty() && LEAVES.stream().allMatch(Entity::isAlive) && have.size() == LEAVES.size()) return;
        for (Entity e : have) e.discard();
        LEAVES.clear(); BASE.clear(); SIDE.clear();
        // створка: плита + жёлто-чёрные полосы по кромке + иллюминатор
        for (int side = -1; side <= 1; side += 2) {
            double x0 = side < 0 ? -3.5 : 0.0;   // 1.2: проём 7 блоков — без щели слева
            leaf(l, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), x0, 0, 3.5f, 5f, 0.4f, side, 0);
            leaf(l, Blocks.YELLOW_CONCRETE.defaultBlockState(), side < 0 ? -0.25 : 0.0, 0, 0.25f, 5f, 0.5f, side, -0.05);
            for (int k = 0; k < 5; k++) leaf(l, Blocks.BLACK_CONCRETE.defaultBlockState(), side < 0 ? -0.26 : -0.01, k + 0.5, 0.27f, 0.45f, 0.52f, side, -0.06);
            leaf(l, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), x0 + 1.2, 2.6, 1.1f, 1.1f, 0.5f, side, -0.05);
            leaf(l, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), x0, 4.6, 3.5f, 0.4f, 0.55f, side, -0.07);
        }
        moveLeaves(0);
    }

    private static void leaf(ServerLevel l, BlockState st, double x, double y, float sx, float sy, float sz, int side, double dz) {
        Entity e = Rocket.display(l, st, sx, sy, sz);
        if (e == null) return;
        e.removeTag("pvpciv_rocket_seat");
        e.addTag(TAG);
        BlockPos g = gate();
        Vec3 b = new Vec3(g.getX() + 0.5 + x, g.getY() + y, g.getZ() + 0.3 + dz);
        e.moveTo(b.x, b.y, b.z, 0, 0);
        l.addFreshEntity(e);
        LEAVES.add(e); BASE.add(b); SIDE.add(side);
    }

    /** k — насколько открыто (0..1): створки уходят в стороны на 3.4 блока. */
    private static void moveLeaves(double k) {
        for (int i = 0; i < LEAVES.size(); i++) {
            Vec3 b = BASE.get(i).add(SIDE.get(i) * 3.6 * k, 0, 0);
            LEAVES.get(i).teleportTo(b.x, b.y, b.z);
        }
    }

    private static void setGate(ServerLevel l, boolean closed) {
        BlockPos g = gate();
        for (int x = -3; x <= 3; x++) for (int y = 0; y < 5; y++) {
            BlockPos q = g.offset(x, y, 0);
            BlockState want = closed ? Blocks.BARRIER.defaultBlockState() : Blocks.AIR.defaultBlockState();
            if (!l.getBlockState(q).equals(want)) l.setBlock(q, want, 2);
        }
    }

    /** Выход наружу: true — запущено. */
    public static boolean start(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        ensure(p.server);
        if (t >= 0 && t > 14) return false;   // уже летят — следующий цикл
        if (t < 0) {
            t = 0;
            l.playSound(null, gate(), SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 2f, 0.5f);
        }
        BlockPos g = gate();
        ArmorStand seat = new ArmorStand(l, g.getX() + 0.5 + (SEATS.size() - 0.5) * 1.6, g.getY(), g.getZ() - 4.5);
        seat.setInvisible(true); seat.setNoGravity(true); seat.setInvulnerable(true); seat.setSilent(true);
        seat.addTag("pvpciv_rocket_seat");
        Rocket.KEEP.add(seat);
        l.addFreshEntity(seat);
        p.startRiding(seat, true);
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, new Vec3(g.getX() + 0.5, g.getY() + 2, g.getZ() + 30));
        SEATS.put(p.getUUID(), seat);
        return true;
    }

    public static void tick(MinecraftServer s) {
        if (t < 0) return;
        ServerLevel l = CivWorld.f5(s);
        BlockPos g = gate();
        t++;
        // тревога
        if (t < OPEN && t % 8 == 0) {
            l.playSound(null, g, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 2f, 0.6f);
            l.sendParticles(net.minecraft.core.particles.DustParticleOptions.REDSTONE, g.getX() + 0.5, g.getY() + 5.3, g.getZ() + 0.5, 12, 3, 0.1, 0.1, 0);
        }
        if (t == 12) l.playSound(null, g, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 2f, 0.5f);
        if (t >= 12 && t <= OPEN) moveLeaves(Math.min(1, (t - 12) / (double) (OPEN - 12)));
        if (t == OPEN) { setGate(l, false); l.playSound(null, g, SoundEvents.ELYTRA_FLYING, SoundSource.BLOCKS, 2f, 0.8f); }
        if (t > OPEN && t < FLY + 20) l.sendParticles(ParticleTypes.CLOUD, g.getX() + 0.5, g.getY() + 2, g.getZ() - 2, 10, 3, 2, 1.5, 0.25);
        // игроков выносит наружу
        for (var it = SEATS.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            ServerPlayer p = s.getPlayerList().getPlayer(en.getKey());
            Entity seat = en.getValue();
            if (p == null || !p.isAlive()) { seat.discard(); Rocket.KEEP.remove(seat); it.remove(); continue; }
            if (p.getVehicle() != seat) p.startRiding(seat, true);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 10, 4, false, false));
            if (t > OPEN) {
                double v = Math.min(1.6, 0.05 + (t - OPEN) * 0.03);
                seat.teleportTo(seat.getX(), seat.getY() + (t < OPEN + 20 ? 0.02 : 0.05), seat.getZ() + v);
                if (t % 2 == 0) l.sendParticles(ParticleTypes.CLOUD, seat.getX(), seat.getY() + 1, seat.getZ() - 0.5, 3, 0.3, 0.3, 0.3, 0.02);
            }
            if (t == FLY) {
                seat.discard(); Rocket.KEEP.remove(seat); it.remove();
                p.stopRiding();
                com.pvpciv.floor.Belt.arriveOut(p);
            }
        }
        if (t == FLY + 10) l.playSound(null, g, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 2f, 0.5f);
        if (t > FLY + 10 && t <= CLOSE_AT) moveLeaves(1 - Math.min(1, (t - FLY - 10) / (double) (CLOSE_AT - FLY - 10)));
        if (t >= CLOSE_AT) { setGate(l, true); moveLeaves(0); t = -1; }
    }

    /** Выйти с сиденья во время вылета нельзя. */
    public static boolean lockSeat(Entity rider) { return rider instanceof ServerPlayer p && riding(p); }
}
