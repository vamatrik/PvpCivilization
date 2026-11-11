package com.pvpciv.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.*;

/** Мировые данные цивилизации: построено ли общежитие и кто где живёт. */
public class CivData extends SavedData {
    public static class Apartment {
        public int id;
        public int number;
        public int floor;
        public BlockPos door;   // нижняя половина двери
        public BlockPos sign;
        public BlockPos spawn;
        public float yaw;
        @Nullable public UUID owner;
        public String ownerName = "";
        public String npc = "";

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("id", id);
            t.putInt("number", number);
            t.putInt("floor", floor);
            t.put("door", NbtUtils.writeBlockPos(door));
            t.put("sign", NbtUtils.writeBlockPos(sign));
            t.put("spawn", NbtUtils.writeBlockPos(spawn));
            t.putFloat("yaw", yaw);
            if (owner != null) t.putUUID("owner", owner);
            t.putString("ownerName", ownerName);
            t.putString("npc", npc);
            return t;
        }

        static Apartment load(CompoundTag t) {
            Apartment a = new Apartment();
            a.id = t.getInt("id");
            a.number = t.getInt("number");
            a.floor = t.getInt("floor");
            a.door = NbtUtils.readBlockPos(t, "door").orElse(BlockPos.ZERO);
            a.sign = NbtUtils.readBlockPos(t, "sign").orElse(BlockPos.ZERO);
            a.spawn = NbtUtils.readBlockPos(t, "spawn").orElse(BlockPos.ZERO);
            a.yaw = t.getFloat("yaw");
            if (t.hasUUID("owner")) a.owner = t.getUUID("owner");
            a.ownerName = t.getString("ownerName");
            a.npc = t.getString("npc");
            return a;
        }

        public boolean isFree() { return owner == null && npc.isEmpty(); }
    }

    public boolean built = false;
    public int version = 0;
    public final List<Apartment> apartments = new ArrayList<>();
    /** Когда проснётся босс (игровое время мира 1), по id: brute, keeper. -1 — не задано. */
    public final Map<String, Long> bossNext = new HashMap<>();
    public long bossNext(String id) { return bossNext.getOrDefault(id, -1L); }
    public final Map<String, Long> stash = new HashMap<>();
    public final Map<UUID, Long> trackBest = new HashMap<>();
    public long trackRecord = 0;
    public String trackRecordName = "";

    public static CivData get(MinecraftServer server) {
        ServerLevel level = server.overworld();
        return level.getDataStorage().computeIfAbsent(new Factory<>(CivData::new, CivData::load), "pvpciv_civilization");
    }

    public static CivData load(CompoundTag tag, HolderLookup.Provider provider) {
        CivData d = new CivData();
        d.built = tag.getBoolean("built");
        d.version = tag.getInt("version");
        ListTag list = tag.getList("apartments", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) d.apartments.add(Apartment.load(list.getCompound(i)));
        if (tag.contains("bossNext")) d.bossNext.put("brute", tag.getLong("bossNext"));
        CompoundTag bt = tag.getCompound("bossTimers");
        for (String k : bt.getAllKeys()) d.bossNext.put(k, bt.getLong(k));
        CompoundTag st = tag.getCompound("stash");
        for (String k : st.getAllKeys()) d.stash.put(k, st.getLong(k));
        CompoundTag tb = tag.getCompound("trackBest");
        for (String k : tb.getAllKeys()) d.trackBest.put(UUID.fromString(k), tb.getLong(k));
        d.trackRecord = tag.getLong("trackRecord");
        d.trackRecordName = tag.getString("trackRecordName");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("built", built);
        tag.putInt("version", version);
        ListTag list = new ListTag();
        for (Apartment a : apartments) list.add(a.save());
        tag.put("apartments", list);
        CompoundTag bt = new CompoundTag();
        bossNext.forEach(bt::putLong);
        tag.put("bossTimers", bt);
        CompoundTag st = new CompoundTag();
        stash.forEach(st::putLong);
        tag.put("stash", st);
        CompoundTag tb = new CompoundTag();
        trackBest.forEach((k, v) -> tb.putLong(k.toString(), v));
        tag.put("trackBest", tb);
        tag.putLong("trackRecord", trackRecord);
        tag.putString("trackRecordName", trackRecordName);
        return tag;
    }

    @Nullable
    public Apartment byId(int id) {
        return id >= 0 && id < apartments.size() ? apartments.get(id) : null;
    }

    @Nullable
    public Apartment byDoor(BlockPos lower) {
        for (Apartment a : apartments) if (a.door.equals(lower)) return a;
        return null;
    }

    @Nullable
    public Apartment byNpc(String npc) {
        for (Apartment a : apartments) if (a.npc.equals(npc)) return a;
        return null;
    }

    @Nullable
    public Apartment byOwner(UUID uuid) {
        for (Apartment a : apartments) if (uuid.equals(a.owner)) return a;
        return null;
    }

    /** Свободная квартира: сначала нижние этажи, и поближе к Луне. */
    @Nullable
    public Apartment findFree() {
        Apartment luna = byNpc("luna");
        BlockPos ref = luna != null ? luna.door : BlockPos.ZERO;
        return apartments.stream().filter(Apartment::isFree)
                .min(Comparator.<Apartment>comparingInt(a -> a.floor).thenComparingDouble(a -> a.door.distSqr(ref)))
                .orElse(null);
    }
}
