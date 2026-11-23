package com.pvpciv.game;

import com.pvpciv.combat.Mobs;
import com.pvpciv.floor.Floor1;
import com.pvpciv.loot.LootTables;
import com.pvpciv.progress.Progress;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Данж «Пепельная Арена»: 3 волны мобов 2–4 ур., лут падает на пол — кто подобрал, того и лут. */
public final class ArenaManager {
    private static final int FLINT = 0xFFB020;
    private static final long COOLDOWN = 20L * 60 * 10;
    private static boolean running;
    private static int wave, delay;
    private static long readyAt = 0;
    private static final List<UUID> mobs = new ArrayList<>();
    /** 1.1.1: Пепельный надзиратель — свои приёмы (в углу его больше не зажать). */
    private static UUID warden;
    private static int wt, slamAt = -1, leapAt = -1;
    private static boolean summoned;

    private static Vec3 center() { return Vec3.atBottomCenterOf(Floor1.ARENA); }

    private static List<ServerPlayer> inside(ServerLevel lvl) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : lvl.players()) if (p.isAlive() && !p.isSpectator() && p.position().distanceTo(center()) < 19.5) out.add(p);
        return out;
    }

    private static void flint(ServerLevel lvl, String msg) {
        for (ServerPlayer p : lvl.players())
            if (p.position().distanceTo(center()) < 60) Story.say(p, "Флинт Громогласный", FLINT, msg);
    }

    public static void tryStart(ServerPlayer p) {
        ServerLevel lvl = p.serverLevel();
        if (running) {
            p.displayClientMessage(Component.literal("Бой уже идёт!").withStyle(ChatFormatting.RED), true);
            return;
        }
        long now = lvl.getGameTime();
        if (now < readyAt) {
            p.displayClientMessage(Component.literal("Ядро остывает. Ещё " + ((readyAt - now) / 1200 + 1) + " мин.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        running = true;
        wave = 0;
        delay = 60;
        mobs.clear();
        gate(lvl, true);
        lvl.playSound(null, Floor1.ARENA, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2f, 0.8f);
        int n = inside(lvl).size();
        flint(lvl, "ДАМЫ И ГОСПОДА! На Пепельной Арене " + (n > 1 ? "целая банда: " + n + " бойцов" : "смельчак " + p.getGameProfile().getName()) + "! Три волны, ставки сделаны!");
        for (ServerPlayer pl : inside(lvl)) Story.title(pl, "ПЕПЕЛЬНАЯ АРЕНА", "Волна 1 из 3", 0xFF8C42);
    }

    private static void gate(ServerLevel lvl, boolean close) {
        for (BlockPos pos : Floor1.arenaGateCells())
            lvl.setBlock(pos, close ? Blocks.IRON_BARS.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    public static void tick(MinecraftServer server) {
        if (!running) return;
        ServerLevel lvl = com.pvpciv.world.CivWorld.level(server);
        if (inside(lvl).isEmpty()) { fail(lvl); return; }
        if (delay > 0) {
            if (--delay == 0) spawnWave(lvl);
            return;
        }
        mobs.removeIf(id -> { Entity e = lvl.getEntity(id); return e == null || !e.isAlive(); });
        // пауки лезут на стены арены — вернуть на песок
        for (UUID id : mobs) if (lvl.getEntity(id) instanceof Mob m && (m.getY() > Floor1.Y + 2.5 || m.position().distanceTo(center()) > 21.5)) {   // бета 12.11: шире — не дёргать мобов у самого края
            Vec3 c = center();
            m.teleportTo(c.x + (m.getRandom().nextDouble() - 0.5) * 10, Floor1.Y, c.z + (m.getRandom().nextDouble() - 0.5) * 10);
            lvl.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + 0.5, m.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
        }
        wardenTick(lvl);
        if (mobs.isEmpty()) {
            wave++;
            if (wave >= 3) { win(lvl); return; }
            delay = 60;
            flint(lvl, wave == 1 ? "Ого, ещё стоят! Выпускайте лучников!" : "А теперь... ПЕПЕЛЬНЫЙ НАДЗИРАТЕЛЬ! Толпа ревёт!");
            for (ServerPlayer pl : inside(lvl)) Story.title(pl, "ВОЛНА " + (wave + 1), wave == 2 ? "Пепельный надзиратель" : "", 0xFF5555);
        }
    }

    private static Vec3 ring(ServerLevel lvl) {
        double a = lvl.random.nextDouble() * Math.PI * 2;
        return center().add(Math.cos(a) * 12, 0, Math.sin(a) * 12);
    }

    public static boolean isArena(Mob m) { return mobs.contains(m.getUUID()); }

    private static void add(Mob m) { if (m != null) { mobs.add(m.getUUID()); m.setTarget(null); } }

    private static void spawnWave(ServerLevel lvl) {
        int players = Math.max(1, inside(lvl).size());
        if (wave == 0) for (int i = 0; i < 3 + players; i++)
            add(Mobs.spawn(lvl, i % 2 == 0 ? Mobs.Kind.ASH_ZOMBIE : Mobs.Kind.WITHERED, 2 + lvl.random.nextInt(2), false, ring(lvl), "floor1_dungeon", "civ_dungeon"));
        if (wave == 1) for (int i = 0; i < 4 + players; i++) {
            Mobs.Kind k = switch (i % 3) { case 0 -> Mobs.Kind.ASH_ARCHER; case 1 -> Mobs.Kind.ASH_SPIDER; default -> Mobs.Kind.WITHERED; };
            add(Mobs.spawn(lvl, k, 3 + lvl.random.nextInt(2), false, ring(lvl), "floor1_dungeon", "civ_dungeon"));
        }
        if (wave == 2) {
            for (int i = 0; i < 1 + players; i++) add(Mobs.spawn(lvl, Mobs.Kind.ASH_ZOMBIE, 4, true, ring(lvl), "floor1_mob", "civ_dungeon"));
            Mob boss = Mobs.spawn(lvl, Mobs.Kind.WITHERED, 5, true, center().add(0, 0, 8), "floor1_mob", "civ_dungeon");
            if (boss != null) {
                Mobs.setup(boss, 5, true, "Пепельный надзиратель", "floor1_mob");
                boss.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE).setBaseValue(1.5);
                add(boss);
                warden = boss.getUUID(); wt = 0; slamAt = -1; leapAt = -1; summoned = false;
            }
            lvl.playSound(null, Floor1.ARENA, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2f, 0.9f);
        }
        for (UUID id : mobs) if (lvl.getEntity(id) instanceof Mob m) lvl.sendParticles(ParticleTypes.LARGE_SMOKE, m.getX(), m.getY() + 1, m.getZ(), 15, 0.3, 0.6, 0.3, 0.02);
        lvl.playSound(null, Floor1.ARENA, SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 2f, 0.7f);
    }

    /**
     * Надзиратель: «Пепельный удар» — если рядом кто-то стоит, через 1 с (дымное кольцо-предупреждение) бьёт по площади и отбрасывает;
     * «Прыжок» — раз в ~9 с прыгает к дальнему игроку и приземляется с ударом; на половине здоровья зовёт двух пепельных зомби.
     */
    private static void wardenTick(ServerLevel lvl) {
        if (warden == null || !(lvl.getEntity(warden) instanceof Mob w) || !w.isAlive()) { warden = null; return; }
        wt++;
        List<ServerPlayer> ps = inside(lvl);
        if (ps.isEmpty()) return;
        // удар по площади: телеграф 20 тиков
        if (slamAt < 0 && wt % 70 == 0 && ps.stream().anyMatch(p -> p.distanceTo(w) < 4.5)) {
            slamAt = wt + 20;
            lvl.playSound(null, w.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 1.5f, 0.6f);
        }
        if (slamAt > 0 && wt < slamAt && wt % 3 == 0)
            for (int i = 0; i < 24; i++) { double a = i * Math.PI / 12; lvl.sendParticles(ParticleTypes.LARGE_SMOKE, w.getX() + Math.cos(a) * 4, w.getY() + 0.1, w.getZ() + Math.sin(a) * 4, 1, 0, 0, 0, 0); }
        if (slamAt > 0 && wt >= slamAt) {
            slamAt = -1;
            lvl.sendParticles(ParticleTypes.EXPLOSION, w.getX(), w.getY() + 0.3, w.getZ(), 6, 2, 0.2, 2, 0);
            lvl.sendParticles(ParticleTypes.LAVA, w.getX(), w.getY() + 0.3, w.getZ(), 20, 2, 0.2, 2, 0);
            lvl.playSound(null, w.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.4f, 0.8f);
            for (ServerPlayer p : ps) if (p.distanceTo(w) < 4.5) {
                p.hurt(lvl.damageSources().mobAttack(w), (float) (w.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 1.6));
                Vec3 push = p.position().subtract(w.position()).multiply(1, 0, 1).normalize().scale(1.4);
                p.setDeltaMovement(push.x, 0.55, push.z); p.hurtMarked = true;
            }
        }
        // прыжок к дальнему
        if (leapAt < 0 && wt % 180 == 90) {
            ServerPlayer far = ps.stream().max(java.util.Comparator.comparingDouble(p -> p.distanceToSqr(w))).orElse(null);
            if (far != null && far.distanceTo(w) > 5) {
                leapAt = wt + 14;
                Vec3 d = far.position().subtract(w.position());
                w.setDeltaMovement(d.x * 0.11, 0.9, d.z * 0.11); w.hurtMarked = true;
                lvl.playSound(null, w.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.4f, 1.2f);
            }
        }
        if (leapAt > 0 && wt >= leapAt && w.onGround()) { leapAt = -1; slamAt = wt + 1; }
        // на половине — подмога
        if (!summoned && w.getHealth() < w.getMaxHealth() * 0.5f) {
            summoned = true;
            for (int i = 0; i < 2; i++) add(Mobs.spawn(lvl, Mobs.Kind.ASH_ZOMBIE, 4, false, ring(lvl), "floor1_dungeon", "civ_dungeon"));
            flint(lvl, "Надзиратель свистнул — с трибун прыгают ещё двое!");
        }
    }

    private static void cleanup(ServerLevel lvl) {
        warden = null;
        for (UUID id : mobs) { Entity e = lvl.getEntity(id); if (e != null) e.discard(); }
        mobs.clear();
        gate(lvl, false);
        running = false;
    }

    private static void fail(ServerLevel lvl) {
        flint(lvl, "И-и-и они сдулись! Толпа освистывает! Арена снова свободна.");
        cleanup(lvl);
    }

    private static void win(ServerLevel lvl) {
        List<ServerPlayer> winners = inside(lvl);
        cleanup(lvl);
        readyAt = lvl.getGameTime() + COOLDOWN;
        Vec3 c = center();
        flint(lvl, "ПОБЕДА! Награда на полу — кто первый, того и лут! Ядро остынет через 10 минут.");
        lvl.playSound(null, Floor1.ARENA, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 2f, 1f);
        lvl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y + 1.5, c.z, 120, 1.5, 1, 1.5, 0.4);
        for (ItemStack st : LootTables.roll("floor1_dungeon_clear", lvl.random, 1.0 + 0.25 * (winners.size() - 1))) {
            ItemEntity ie = new ItemEntity(lvl, c.x, c.y + 1.2, c.z, st);
            ie.setDeltaMovement((lvl.random.nextDouble() - 0.5) * 0.6, 0.45, (lvl.random.nextDouble() - 0.5) * 0.6);
            lvl.addFreshEntity(ie);
            Story.announceRareDrop(lvl, st, "Арена (победители)");
        }
        for (ServerPlayer p : winners) {
            Progress.addXp(p, Progress.xpFor(Progress.d(p).level, 4, 8));
            Story.title(p, "ПОБЕДА", "Лут на полу арены", 0xFFD166);
        }
    }

    public static void onLogout(ServerPlayer p) {}
}
