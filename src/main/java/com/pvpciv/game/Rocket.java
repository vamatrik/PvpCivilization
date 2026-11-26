package com.pvpciv.game;

import com.pvpciv.floor.Floor4;
import com.pvpciv.floor.Floor5;
import com.pvpciv.net.Network;
import com.pvpciv.world.CivWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 0.12 — ракета «Заря-5»: космодром NaZa (этаж 4) ↔ ангар станции «Орбита» (этаж 5).
 * Открывается после победы над доктором Ноэлем (трофей 64). Полёт — кат-сцена ~6 с: отсчёт, тряска камеры, белая вспышка,
 * на станции — проявление из темноты (PhoneData 22). Пока игрок «в ракете», он не двигается и не получает урон.
 */
public final class Rocket {
    private Rocket() {}
    /** 1.1: посадка (отсчёт) — T0 тиков, затем подъём; на LEN — прибытие. */
    public static final int T0 = 70, LEN = 170;
    private static final double ACC = 0.012;

    /** Один старт с площадки: ракета из блок-дисплеев, сиденья-камеры пассажиров. */
    private static final class Launch {
        final boolean up; final ServerLevel l; final BlockPos origin; int t;
        final List<Entity> parts = new ArrayList<>(); final List<Vec3> offs = new ArrayList<>();
        final Map<UUID, Entity> seats = new HashMap<>();
        Launch(boolean up, ServerLevel l, BlockPos origin) { this.up = up; this.l = l; this.origin = origin; }
        double rise() { return t < T0 ? 0 : ACC * (t - T0) * (t - T0); }
    }
    private static final Map<Boolean, Launch> PADS = new HashMap<>();
    private static final Map<UUID, Launch> RIDERS = new HashMap<>();
    /** Когда восстановить блок-ракету (игровое время), по площадке. */
    private static final Map<Boolean, Long> RESTORE = new HashMap<>();
    /** 1.2: люк в крыше ангара (этаж 5) — на время старта: позиция → блок. */
    private static final Map<BlockPos, BlockState> HATCH = new java.util.LinkedHashMap<>();

    private static void hatch(ServerLevel l, BlockPos o, boolean open) {
        if (open) {
            for (int y = 12; y <= 24; y++) for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
                if (Math.hypot(x, z) > 4.2) continue;
                BlockPos q = o.offset(x, y, z);
                BlockState st = l.getBlockState(q);
                if (st.isAir()) continue;
                HATCH.put(q, st);
                l.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
            }
            l.playSound(null, o.above(14), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 3f, 0.5f);
            l.sendParticles(ParticleTypes.CLOUD, o.getX() + 0.5, o.getY() + 14, o.getZ() + 0.5, 60, 3, 0.5, 3, 0.05);
        } else {
            for (var e : HATCH.entrySet()) l.setBlock(e.getKey(), e.getValue(), 2);
            if (!HATCH.isEmpty()) l.playSound(null, o.above(14), SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 3f, 0.5f);
            HATCH.clear();
        }
    }

    public static boolean unlocked(com.pvpciv.data.StoryData d) { return (d.trophies & 64) != 0; }
    public static boolean flying(ServerPlayer p) { return RIDERS.containsKey(p.getUUID()); }

    /** 1.2: перестроить ракету на месте (лестница, люк, кабина) — для старых миров. */
    public static void rebuild(MinecraftServer s) {
        for (boolean up : new boolean[]{true, false}) {
            if (PADS.containsKey(up)) continue;
            ServerLevel l = up ? CivWorld.f4(s) : CivWorld.f5(s);
            if (l == null) continue;
            BlockPos o = origin(up);
            for (var v : voxels(up).entrySet()) l.setBlock(o.offset(v.getKey()), v.getValue(), 2);
            for (BlockPos h : up ? Floor4.ROCKET_HATCH : Floor5.ROCKET_HATCH) l.setBlock(o.offset(h), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static BlockPos origin(boolean up) { return up ? Floor4.ROCKET() : Floor5.ROCKET(); }
    private static Map<BlockPos, BlockState> voxels(boolean up) { return up ? Floor4.rocketVoxels() : Floor5.rocketVoxels(); }

    /** ПКМ по блоку: пульт космодрома (этаж 4) или ангара (этаж 5). */
    public static boolean onUse(ServerPlayer p, BlockPos pos) {
        boolean f4 = CivWorld.isF4(p.level()) && pos.getZ() == Floor4.COSMO.getZ() - 16 && Math.abs(pos.getX() - Floor4.COSMO.getX()) <= 3
                && (pos.getY() == Floor4.Y || pos.getY() == Floor4.Y + 1);
        boolean f5 = CivWorld.isF5(p.level()) && (pos.equals(Floor5.HANGAR_CONSOLE()) || pos.equals(Floor5.ROCKET().offset(Floor5.ROCKET_CONSOLE_OFF)));
        f4 |= CivWorld.isF4(p.level()) && pos.equals(Floor4.ROCKET().offset(Floor4.ROCKET_CONSOLE_OFF));   // 1.2: пульт в кабине
        if (!f4 && !f5) return false;
        if (flying(p)) return true;
        var d = com.pvpciv.progress.Progress.d(p);
        if (f4 && !unlocked(d)) {
            Story.say(p, "Пульт «Заря-5»", 0xFF8A30, "СТАРТ ЗАБЛОКИРОВАН. Ключ запуска — у руководства NaZa. (Победи доктора Ноэля в «Ковчеге».)");
            return true;
        }
        Launch L = PADS.get(f4);
        if (L != null && L.t >= T0 - 10) {
            p.displayClientMessage(Component.literal("Ракета уже стартует. Следующая — через несколько секунд.").withStyle(ChatFormatting.GOLD), true);
            return true;
        }
        if (L == null) { L = start(f4, p.serverLevel()); PADS.put(f4, L); }
        board(L, p);
        p.displayClientMessage(Component.literal(f4 ? "«Заря-5»: курс — станция «Орбита»" : "«Заря-5»: курс — космодром NaZa").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    private static Launch start(boolean up, ServerLevel l) {
        Launch L = new Launch(up, l, origin(up));
        PADS.put(up, L);   // до создания дисплеев — иначе их отсеет проверка «остатков»
        Map<BlockPos, BlockState> vox = voxels(up);
        // блоки ракеты → воздух, вместо них — подвижные блок-дисплеи (слитые в бруски)
        for (BlockPos q : vox.keySet()) l.setBlock(L.origin.offset(q), Blocks.AIR.defaultBlockState(), 2);
        for (int[] b : merge(vox)) {
            BlockState st = Block.stateById(b[6]);
            Entity e = display(l, st, b[3] - b[0] + 1, b[4] - b[1] + 1, b[5] - b[2] + 1);
            if (e == null) continue;
            Vec3 off = new Vec3(b[0], b[1], b[2]);
            Vec3 at = Vec3.atLowerCornerOf(L.origin).add(off);
            e.moveTo(at.x, at.y, at.z, 0, 0);
            L.parts.add(e); L.offs.add(off);
            l.addFreshEntity(e);
        }
        RESTORE.remove(up);
        return L;
    }

    /** Блок-дисплей с масштабом (уголок — в позиции сущности). Помечен тегом — после перезапуска не возвращается. */
    public static Entity display(ServerLevel l, BlockState st, float sx, float sy, float sz) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:block_display");
        tag.put("block_state", NbtUtils.writeBlockState(st));
        tag.putInt("teleport_duration", 2);
        tag.putFloat("view_range", 4f);
        CompoundTag tr = new CompoundTag();
        tr.put("left_rotation", floats(0, 0, 0, 1)); tr.put("right_rotation", floats(0, 0, 0, 1));
        tr.put("translation", floats(0, 0, 0)); tr.put("scale", floats(sx, sy, sz));
        tag.put("transformation", tr);
        net.minecraft.nbt.ListTag tags = new net.minecraft.nbt.ListTag(); tags.add(net.minecraft.nbt.StringTag.valueOf("pvpciv_rocket_seat")); tag.put("Tags", tags);
        return EntityType.loadEntityRecursive(tag, l, x -> x);
    }
    /** Временные сущности других сцен (шахта лифта) — тоже «живые», не сносить. */
    public static final java.util.Set<Entity> KEEP = new java.util.HashSet<>();

    private static net.minecraft.nbt.ListTag floats(float... v) {
        net.minecraft.nbt.ListTag t = new net.minecraft.nbt.ListTag();
        for (float f : v) t.add(net.minecraft.nbt.FloatTag.valueOf(f));
        return t;
    }

    /** Сливает одинаковые блоки в бруски: по x, затем по y. [x0,y0,z0,x1,y1,z1,stateId]. */
    private static List<int[]> merge(Map<BlockPos, BlockState> vox) {
        Map<String, int[]> runs = new java.util.LinkedHashMap<>();
        List<int[]> xr = new ArrayList<>();
        java.util.Set<BlockPos> done = new java.util.HashSet<>();
        List<BlockPos> keys = new ArrayList<>(vox.keySet());
        keys.sort(java.util.Comparator.<BlockPos>comparingInt(q -> q.getY()).thenComparingInt(q -> q.getZ()).thenComparingInt(q -> q.getX()));
        for (BlockPos k : keys) {
            if (done.contains(k)) continue;
            BlockState st = vox.get(k);
            int x1 = k.getX();
            while (st.equals(vox.get(new BlockPos(x1 + 1, k.getY(), k.getZ())))) { x1++; done.add(new BlockPos(x1, k.getY(), k.getZ())); }
            done.add(k);
            xr.add(new int[]{k.getX(), k.getY(), k.getZ(), x1, k.getY(), k.getZ(), Block.getId(st)});
        }
        // по y: тот же x-диапазон, z и блок на следующем слое — продлить
        List<int[]> out = new ArrayList<>();
        for (int[] r : xr) {
            String key = r[0] + "," + r[3] + "," + r[2] + "," + r[6];
            int[] prev = runs.get(key);
            if (prev != null && prev[4] == r[1] - 1) { prev[4] = r[1]; continue; }
            int[] c = r.clone(); runs.put(key, c); out.add(c);
        }
        return out;
    }

    private static void board(Launch L, ServerPlayer p) {
        Vec3 o = Vec3.atBottomCenterOf(L.origin);
        // камера — со стороны пульта, в ~14 блоках от ракеты
        Vec3 dir = new Vec3(p.getX() - o.x, 0, p.getZ() - o.z);
        if (dir.lengthSqr() < 1) dir = new Vec3(0, 0, -1);
        dir = dir.normalize().yRot((float) (L.seats.size() * 0.35));
        double dist = L.up ? 16 : 9;
        Vec3 at = o.add(dir.scale(dist)).add(0, L.up ? 3 : 1.5, 0);
        ArmorStand seat = new ArmorStand(L.l, at.x, at.y, at.z);
        seat.setInvisible(true); seat.setNoGravity(true); seat.setInvulnerable(true); seat.setSilent(true);
        seat.addTag("pvpciv_rocket_seat");
        L.seats.put(p.getUUID(), seat);
        L.l.addFreshEntity(seat);
        p.startRiding(seat, true);
        RIDERS.put(p.getUUID(), L);
        double h = L.up ? 18 : 6;
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(22, String.format(java.util.Locale.ROOT,
                "{\"cut\":\"launch\",\"len\":%d,\"t0\":%d,\"acc\":%.4f,\"x\":%.2f,\"y\":%.2f,\"z\":%.2f,\"skip\":%d}",
                LEN, T0, ACC, o.x, o.y + h, o.z, L.t)));
        L.l.playSound(null, p.blockPosition(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.PLAYERS, 1f, 0.7f);
    }

    public static void tick(MinecraftServer server) {
        // восстановить блок-ракету после старта
        RESTORE.entrySet().removeIf(en -> {
            ServerLevel l = en.getKey() ? CivWorld.f4(server) : CivWorld.f5(server);
            if (l.getGameTime() < en.getValue() || PADS.containsKey(en.getKey())) return false;
            BlockPos o = origin(en.getKey());
            for (var v : voxels(en.getKey()).entrySet()) l.setBlock(o.offset(v.getKey()), v.getValue(), 2);
            if (!en.getKey()) hatch(l, o, false);
            l.sendParticles(ParticleTypes.CLOUD, o.getX() + 0.5, o.getY() + 2, o.getZ() + 0.5, 40, 2, 1, 2, 0.02);
            return true;
        });
        PADS.values().removeIf(L -> {
            L.t++;
            int t = L.t;
            double rise = L.rise();
            for (int i = 0; i < L.parts.size(); i++) {
                Vec3 at = Vec3.atLowerCornerOf(L.origin).add(L.offs.get(i)).add(0, rise, 0);
                L.parts.get(i).teleportTo(at.x, at.y, at.z);
            }
            Vec3 base = Vec3.atBottomCenterOf(L.origin).add(0, rise + 0.5, 0);
            // пассажиры
            for (var it = L.seats.entrySet().iterator(); it.hasNext(); ) {
                var en = it.next();
                ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
                Entity seat = en.getValue();
                if (p == null || !p.isAlive()) { seat.discard(); RIDERS.remove(en.getKey()); it.remove(); continue; }
                if (p.getVehicle() != seat) p.startRiding(seat, true);
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 4, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 10, 0, false, false));
                if (t > T0) seat.teleportTo(seat.getX(), seat.getY() + Math.min(0.9, rise * 0.02), seat.getZ());   // камера чуть следует за ракетой
                if (t == 20 || t == 40 || t == 60) {
                    int n = 4 - t / 20;
                    p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal(String.valueOf(n)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
                    L.l.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1f, 0.8f + n * 0.1f);
                }
                if (t == T0) p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("ПУСК").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
            }
            if (t == 1) L.l.playSound(null, BlockPos.containing(base), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 2f, 0.6f);
            if (t == T0 - 25 && !L.up) hatch(L.l, L.origin, true);   // 1.2: раньше ракета улетала сквозь потолок ангара
            if (t > T0 - 15) {
                double k = Math.min(1, (t - T0 + 15) / 20.0);
                L.l.sendParticles(ParticleTypes.FLAME, base.x, base.y, base.z, (int) (8 + 25 * k), 0.8, 0.2, 0.8, 0.03 + 0.08 * k);
                L.l.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, base.x, Math.max(L.origin.getY() + 0.5, base.y - 1), base.z, (int) (4 + 8 * k), 2.5, 0.5, 2.5, 0.03);
                if (t > T0) L.l.sendParticles(ParticleTypes.LAVA, base.x, base.y - 0.5, base.z, 3, 0.4, 0.2, 0.4, 0);
                if (t % 4 == 0) L.l.playSound(null, BlockPos.containing(base), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2f, 0.5f);
            }
            if (t == T0) {
                L.l.playSound(null, BlockPos.containing(base), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 3f, 0.5f);
                L.l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, base.x, base.y, base.z, 2, 1, 0.2, 1, 0);
            }
            if (t < LEN) return false;
            // прибытие
            for (var en : L.seats.entrySet()) {
                ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
                RIDERS.remove(en.getKey());
                en.getValue().discard();
                if (p == null) continue;
                p.stopRiding();
                if (L.up) {
                    ServerLevel to = CivWorld.f5(server);
                    Vec3 a = Floor5.HANGAR_ARRIVE();
                    p.teleportTo(to, a.x, a.y, a.z, 180, 0);
                    Floor5.onArrive(p);
                } else {
                    ServerLevel to = CivWorld.f4(server);
                    p.teleportTo(to, Floor4.COSMO.getX() + 0.5, Floor4.Y, Floor4.COSMO.getZ() - 20.5, 180, 0);
                }
                p.resetFallDistance();
                p.removeEffect(MobEffects.INVISIBILITY);
                PacketDistributor.sendToPlayer(p, new Network.PhoneData(22, "{\"cut\":\"arrive\",\"len\":30}"));
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal(L.up ? "Станция «Орбита»" : "Космодром NaZa").withStyle(ChatFormatting.LIGHT_PURPLE)));
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal(L.up ? "Этаж 5" : "Этаж 4").withStyle(ChatFormatting.GRAY)));
            }
            for (Entity e : L.parts) e.discard();
            RESTORE.put(L.up, L.l.getGameTime() + 100);
            return true;
        });
    }

    /** Выход с сиденья во время полёта запрещён. */
    public static boolean lockSeat(Entity rider, Entity vehicle) { return rider instanceof ServerPlayer p && flying(p) && vehicle.getTags().contains("pvpciv_rocket_seat"); }

    /** Остатки старта после перезапуска сервера: не пускать в мир. */
    public static boolean stale(Entity e) {
        if (!e.getTags().contains("pvpciv_rocket_seat")) return false;
        if (KEEP.contains(e)) return false;
        for (Launch L : PADS.values()) if (L.parts.contains(e) || L.seats.containsValue(e)) return false;
        return true;
    }

    /** При старте сервера: если ракета осталась «улетевшей» (сервер выключили в полёте) — вернуть блоки. */
    public static void startup(MinecraftServer server) {
        for (boolean up : new boolean[]{true, false}) {
            ServerLevel l = up ? CivWorld.f4(server) : CivWorld.f5(server);
            BlockPos o = origin(up);
            var vox = voxels(up);
            BlockPos first = vox.keySet().iterator().next();
            if (l.getBlockState(o.offset(first)).isAir()) for (var v : vox.entrySet()) l.setBlock(o.offset(v.getKey()), v.getValue(), 2);
        }
    }
}
