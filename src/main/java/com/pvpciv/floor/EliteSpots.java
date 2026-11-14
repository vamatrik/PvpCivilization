package com.pvpciv.floor;

import com.pvpciv.combat.Mobs;
import com.pvpciv.world.CivWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Элитные точки: на каждой живёт один именной элитный моб с улучшенным лутом. Убит — возрождается через 3–5 минут,
 * над точкой висит голограмма с таймером. Там же — голограммы боссов. При запуске сервера все живы.
 */
public final class EliteSpots {
    public record Spot(String id, BlockPos pos, Mobs.Kind kind, int level, String name, String loot, boolean f2) {
        public Spot(String id, BlockPos pos, Mobs.Kind kind, int level, String name, String loot) { this(id, pos, kind, level, name, loot, false); }
    }

    /** Группы лута, где вторые элитки разнесены по локации: Пепельная пустошь, деревня самураев. */
    private static final java.util.Set<String> ROAM = java.util.Set.of("floor1_mob", "floor3_mob");

    public static final List<Spot> SPOTS = twins(List.of(
            new Spot("e_hall", new BlockPos(Floor1.RUINS.getX(), Floor1.Y, Floor1.RUINS.getZ() - 20), Mobs.Kind.WITHERED, 4, "Пепельный капрал", "floor1_mob"),
            new Spot("e_grove", Floor1.GROVE.offset(5, 0, 3), Mobs.Kind.ASH_SPIDER, 4, "Хозяйка рощи", "floor1_mob"),
            new Spot("e_pillars", Floor1.PILLARS.offset(-4, 0, 6), Mobs.Kind.ASH_ARCHER, 4, "Снайпер столпов", "floor1_mob"),
            new Spot("e_spires", FloorNorth.SPIRES.offset(-5, 0, 4), Mobs.Kind.FROST_ZOMBIE, 7, "Хрустальный страж", "floor1b_mob"),
            new Spot("e_pines", FloorNorth.PINES.offset(4, 0, -5), Mobs.Kind.FROST_ARCHER, 7, "Егерь кладбища", "floor1b_mob"),
            new Spot("e_crypt1", FloorNorth.CRYPT.offset(-5, 0, -3), Mobs.Kind.FROST_ZOMBIE, 7, "Страж склепа", "floor1b_mob"),
            new Spot("e_crypt2", FloorNorth.CRYPT.offset(5, 0, -3), Mobs.Kind.FROST_ARCHER, 7, "Лучница склепа", "floor1b_mob")));

    /**
     * По две элиты на точку, но вторая — ДРУГОГО вида из той же части этажа (бета 12.11: одинаковые элиты не стоят парой).
     * Уровни не меняются: дальше от старта — сильнее.
     */
    public static List<Spot> twins(List<Spot> base) {
        List<Spot> out = new java.util.ArrayList<>(base);
        for (String grp : base.stream().map(Spot::loot).distinct().toList()) {
            List<Spot> g = base.stream().filter(s -> s.loot().equals(grp)).toList();
            for (int i = 0; i < g.size(); i++) {
                Spot s = g.get(i), o = g.get((i + 1) % g.size());
                for (int j = 1; j < g.size() && o.kind() == s.kind(); j++) o = g.get((i + 1 + j) % g.size());
                // имя — по виду (у исходной элиты имя привязано к её месту: «Хозяйка рощи» у столпов звучала бы странно)
                // 0.9.0: на «открытых» локациях вторая элитка бродит отдельно (20–32 блока от первой), на остальных — парой
                BlockPos tp;
                if (ROAM.contains(grp)) {
                    double a = Math.floorMod(s.id().hashCode(), 360) * Math.PI / 180, r = 20 + Math.floorMod(s.id().hashCode() >> 8, 13);
                    tp = s.pos().offset((int) Math.round(Math.cos(a) * r), 0, (int) Math.round(Math.sin(a) * r));
                    if (grp.equals("floor1_mob")) tp = new BlockPos(Math.max(Floor1.X0 + 8, Math.min(Floor1.X1 - 8, tp.getX())), tp.getY(), Math.max(Floor1.Z0 + 12, Math.min(Floor1.Z1 - 8, tp.getZ())));
                    else {   // деревня самураев: не дальше края острова
                        BlockPos c = Floor3Areas.SV; double dx = tp.getX() - c.getX(), dz = tp.getZ() - c.getZ(), dd = Math.hypot(dx, dz), max = Floor3Areas.SV_R - 10;
                        if (dd > max) tp = new BlockPos(c.getX() + (int) (dx / dd * max), tp.getY(), c.getZ() + (int) (dz / dd * max));
                    }
                } else tp = s.pos().offset(s.id().contains("crypt") ? 0 : 6, 0, s.id().contains("crypt") ? 5 : 3);
                out.add(new Spot(s.id() + "_b", tp, o.kind(), s.level(), "Элитный " + Character.toLowerCase(o.kind().title.charAt(0)) + o.kind().title.substring(1), s.loot(), s.f2()));
            }
        }
        return out;
    }

    private static final Map<String, Long> RESPAWN = new HashMap<>();

    private static String tag(Spot s) { return "civelite_" + s.id(); }

    /** Элитный моб с точки убит — запомнить время возрождения. */
    public static void onDeath(Mob m) {
        for (Spot s : java.util.stream.Stream.of(SPOTS.stream(), Floor2.ELITES.stream(), Floor3Areas.ELITES.stream(), Floor4.ELITES.stream()).flatMap(x -> x).toList()) if (m.getTags().contains(tag(s)))
            RESPAWN.put(s.id(), m.level().getGameTime() + 20L * (180 + m.getRandom().nextInt(121)));
    }

    public static void tick(MinecraftServer server) {
        tick(server, CivWorld.level(server), SPOTS);
        tick(server, CivWorld.f2(server), Floor2.ELITES);
        if (CivWorld.isF3(CivWorld.f3(server))) tick(server, CivWorld.f3(server), Floor3Areas.ELITES);
        if (CivWorld.isF4(CivWorld.f4(server))) tick(server, CivWorld.f4(server), Floor4.ELITES);
        bossHolograms(server);
    }

    private static void tick(MinecraftServer server, ServerLevel lvl, List<Spot> spots) {
        long now = lvl.getGameTime();
        for (Spot s : spots) {
            Vec3 c = Vec3.atBottomCenterOf(s.pos());
            boolean near = false;
            for (ServerPlayer p : lvl.players()) if (p.position().distanceTo(c) < 64) { near = true; break; }
            if (!near) continue;
            List<Mob> alive = lvl.getEntitiesOfClass(Mob.class, new AABB(s.pos()).inflate(40), m -> m.getTags().contains(tag(s)) && m.isAlive());
            for (int i = 1; i < alive.size(); i++) alive.get(i).discard();
            long at = RESPAWN.getOrDefault(s.id(), 0L);
            if (alive.isEmpty() && at <= now) {
                int y = s.f2() ? s.pos().getY() : lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, s.pos().getX(), s.pos().getZ());
                Vec3 sp = s.f2() ? ground(lvl, BlockPos.containing(c.x, y, c.z)) : com.pvpciv.game.Taxi.safe(lvl, BlockPos.containing(c.x, y, c.z));   // бета 0.8.7: элитка в камне
                Mob m = Mobs.spawn(lvl, s.kind(), s.level(), true, sp, s.loot(), tag(s));
                if (m != null) Mobs.setup(m, s.level(), true, s.name(), s.loot());
                hologram(lvl, "holo_" + s.id(), c.add(0, 2.6, 0), null);
            } else if (!alive.isEmpty()) {
                Mob m = alive.get(0);
                hologram(lvl, "holo_" + s.id(), c.add(0, 2.6, 0), null);
            } else {
                long sec = Math.max(0, (at - now) / 20);
                hologram(lvl, "holo_" + s.id(), c.add(0, 2.6, 0), Component.literal("★ " + s.name() + "\n").withStyle(x -> x.withColor(0xFFD166).withBold(true))
                        .append(Component.literal(String.format("возродится через %d:%02d", sec / 60, sec % 60)).withStyle(x -> x.withColor(0xE8ECF4).withBold(false))));
            }
        }
    }

    private static void bossHolograms(MinecraftServer server) {
        for (BossManager.Def bd : BossManager.DEFS) {
            ServerLevel lvl = BossManager.f4(bd) ? CivWorld.f4(server) : BossManager.f3(bd) ? CivWorld.f3(server) : bd.f2() ? CivWorld.f2(server) : CivWorld.level(server);
            int bs = BossManager.secondsLeft(server, bd.id());
            Vec3 lair = Vec3.atBottomCenterOf(bd.lair().get()).add(0, 4, 0);
            boolean nearLair = false;
            for (ServerPlayer p : lvl.players()) if (p.position().distanceTo(lair) < 80) nearLair = true;
            int col = bd == BossManager.KEEPER ? 0x7FD8FF : 0xFF5566;
            String woke = bd == BossManager.KEEPER ? "просыпается…" : "просыпается…";
            if (nearLair) hologram(lvl, "holo_" + bd.id(), lair, bs < 0 ? null
                    : Component.literal("☠ " + bd.name() + "  ур. " + bd.level() + "\n").withStyle(x -> x.withColor(col).withBold(true))
                    .append(Component.literal(bs == 0 ? woke : String.format("проснётся через %d:%02d", bs / 60, bs % 60)).withStyle(x -> x.withColor(0xE8ECF4).withBold(false))));
        }
    }

    /** Создать/обновить/убрать (text == null) текстовую голограмму с меткой id. */
    private static void hologram(ServerLevel lvl, String id, Vec3 at, Component text) {
        List<Display.TextDisplay> found = lvl.getEntitiesOfClass(Display.TextDisplay.class, new AABB(at, at).inflate(2), e -> e.getTags().contains(id));
        if (text == null) { found.forEach(Entity::discard); return; }
        // голограмма сохраняется с чанком; если её создали, пока чанк догружался, их становилось две (бета 12.11: «два таймера друг на друге»)
        for (int i = 1; i < found.size(); i++) found.get(i).discard();
        Display.TextDisplay d;
        if (found.isEmpty()) {
            d = EntityType.TEXT_DISPLAY.create(lvl);
            if (d == null) return;
            d.moveTo(at.x, at.y, at.z);
            d.addTag(id);
            lvl.addFreshEntity(d);
        } else d = found.get(0);
        CompoundTag t = new CompoundTag();
        d.saveWithoutId(t);
        t.putString("text", Component.Serializer.toJson(text, lvl.registryAccess()));
        t.putString("billboard", "center");
        t.putInt("background", 0xA0100A14);
        t.putBoolean("shadow", true);
        t.putInt("line_width", 220);
        d.load(t);
    }

    /** Для точек с фиксированной высотой (этажи 2–3): ближайшая клетка, где под ногами пол, а сверху два блока воздуха (не крыша). */
    private static Vec3 ground(ServerLevel l, BlockPos b) {
        for (int r = 0; r <= 8; r++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
            BlockPos p = b.offset(dx, 0, dz);
            if (l.getBlockState(p.below()).isSolid() && l.getBlockState(p).isAir() && l.getBlockState(p.above()).isAir()) return Vec3.atBottomCenterOf(p);
        }
        return com.pvpciv.game.Taxi.safe(l, b);
    }
}
