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

    public static final List<Spot> SPOTS = List.of(
            new Spot("e_hall", new BlockPos(Floor1.RUINS.getX(), Floor1.Y, Floor1.RUINS.getZ() - 20), Mobs.Kind.WITHERED, 4, "Пепельный капрал", "floor1_mob"),
            new Spot("e_grove", Floor1.GROVE.offset(5, 0, 3), Mobs.Kind.ASH_SPIDER, 4, "Хозяйка рощи", "floor1_mob"),
            new Spot("e_pillars", Floor1.PILLARS.offset(-4, 0, 6), Mobs.Kind.ASH_ARCHER, 4, "Снайпер столпов", "floor1_mob"),
            new Spot("e_spires", FloorNorth.SPIRES.offset(-5, 0, 4), Mobs.Kind.FROST_ZOMBIE, 7, "Хрустальный страж", "floor1b_mob"),
            new Spot("e_pines", FloorNorth.PINES.offset(4, 0, -5), Mobs.Kind.FROST_ARCHER, 7, "Егерь кладбища", "floor1b_mob"),
            new Spot("e_crypt1", FloorNorth.CRYPT.offset(-5, 0, -3), Mobs.Kind.FROST_ZOMBIE, 7, "Страж склепа", "floor1b_mob"),
            new Spot("e_crypt2", FloorNorth.CRYPT.offset(5, 0, -3), Mobs.Kind.FROST_ARCHER, 7, "Лучница склепа", "floor1b_mob"));

    private static final Map<String, Long> RESPAWN = new HashMap<>();

    private static String tag(Spot s) { return "civelite_" + s.id(); }

    /** Элитный моб с точки убит — запомнить время возрождения. */
    public static void onDeath(Mob m) {
        for (Spot s : java.util.stream.Stream.concat(SPOTS.stream(), Floor2.ELITES.stream()).toList()) if (m.getTags().contains(tag(s)))
            RESPAWN.put(s.id(), m.level().getGameTime() + 20L * (180 + m.getRandom().nextInt(121)));
    }

    public static void tick(MinecraftServer server) {
        tick(server, CivWorld.level(server), SPOTS);
        tick(server, CivWorld.f2(server), Floor2.ELITES);
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
                Mob m = Mobs.spawn(lvl, s.kind(), s.level(), true, new Vec3(c.x, y, c.z), s.loot(), tag(s));
                if (m != null) Mobs.setup(m, s.level(), true, s.name(), s.loot());
                hologram(lvl, "holo_" + s.id(), c.add(0, 2.6, 0), null);
            } else if (!alive.isEmpty()) {
                Mob m = alive.get(0);
                if (m.position().distanceTo(c) > 14) m.teleportTo(c.x, m.getY(), c.z);   // не уходит далеко от точки
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
            ServerLevel lvl = bd.f2() ? CivWorld.f2(server) : CivWorld.level(server);
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
}
