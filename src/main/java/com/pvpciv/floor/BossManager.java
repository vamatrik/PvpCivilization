package com.pvpciv.floor;

import com.pvpciv.combat.Mobs;
import com.pvpciv.data.CivData;
import com.pvpciv.data.StoryData;
import com.pvpciv.game.Story;
import com.pvpciv.loot.LootTables;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.*;

/** Гнилой Громила — босс этажа 1. Спавн раз в час, землетрясение, личный и общий лут. */
public final class BossManager {
    public static final int LEVEL = 4;
    public static final long PERIOD = 20L * 60 * 60;
    public static final String TAG = "pvpciv_boss";
    private static UUID bossId;
    private static final Map<UUID, Float> DAMAGE = new HashMap<>();
    private static ServerBossEvent bar;
    private static int abilityTimer = 140, telegraph = -1, aliveTicks = 0;
    private static boolean warned5, warned1, minions;

    public static boolean isBoss(LivingEntity e) { return e.getTags().contains(TAG); }

    /** Секунд до спавна (-1 если босс жив). */
    public static int secondsLeft(MinecraftServer server) {
        if (bossId != null) return -1;
        long next = CivData.get(server).bossNext;
        return (int) Math.max(0, (next - server.overworld().getGameTime()) / 20);
    }

    private static void broadcast(MinecraftServer s, String msg, ChatFormatting c) {
        for (ServerPlayer p : s.getPlayerList().getPlayers())
            p.sendSystemMessage(Component.literal("☠ ").withStyle(ChatFormatting.DARK_RED).append(Component.literal(msg).withStyle(c)));
    }

    public static void tick(MinecraftServer server) {
        ServerLevel lvl = server.overworld();
        CivData data = CivData.get(server);
        long now = lvl.getGameTime();
        if (data.bossNext < 0) { data.bossNext = now + 20L * 180; data.setDirty(); }
        boolean lairLoaded = lvl.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(Floor1.LAIR));
        Mob boss = bossId == null ? null : (lvl.getEntity(bossId) instanceof Mob m ? m : null);
        if (bossId == null && lairLoaded && now % 40 == 0) {
            // подхватываем босса, пережившего перезапуск сервера
            List<Mob> found = lvl.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(Floor1.LAIR).inflate(40), m -> m.getTags().contains(TAG));
            for (int i = 1; i < found.size(); i++) found.get(i).discard();
            if (!found.isEmpty()) {
                boss = found.get(0);
                bossId = boss.getUUID();
                bar = new ServerBossEvent(Component.literal("Гнилой Громила • ур. " + LEVEL), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
            }
        }
        if (bossId != null && (boss == null || !boss.isAlive())) {
            if (boss == null && !lairLoaded) return; // игроки ушли — босс ждёт в выгруженном чанке
            cleanup(server, true);
            return;
        }
        if (boss == null) {
            long left = data.bossNext - now;
            if (left <= 20L * 300 && !warned5) { warned5 = true; broadcast(server, "Гнилой Громила проснётся через 5 минут в Логове (этаж 1, PvP)!", ChatFormatting.GOLD); }
            if (left <= 20L * 60 && !warned1) { warned1 = true; broadcast(server, "Громила проснётся через минуту! Готовьте оружие.", ChatFormatting.RED); }
            if (left <= 0) {
                if (!lairLoaded) return; // никого рядом — проснётся, когда кто-то придёт
                spawn(lvl);
            }
            return;
        }
        aliveTicks++;
        if (aliveTicks > 20 * 60 * 30) {
            broadcast(server, "Гнилой Громила заскучал и ушёл спать. Вернётся через час.", ChatFormatting.GRAY);
            boss.discard();
            cleanup(server, false);
            return;
        }
        Vec3 lair = Vec3.atBottomCenterOf(Floor1.LAIR);
        if (boss.position().distanceTo(lair) > Floor1.LAIR_R + 6) {
            lvl.sendParticles(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY() + 1, boss.getZ(), 30, 0.6, 1, 0.6, 0.02);
            boss.teleportTo(lair.x, lair.y, lair.z);
        }
        // полоса здоровья
        bar.setProgress(Math.max(0, boss.getHealth() / boss.getMaxHealth()));
        for (ServerPlayer p : lvl.players()) {
            boolean near = p.position().distanceTo(lair) < 45;
            if (near && !bar.getPlayers().contains(p)) bar.addPlayer(p);
            if (!near && bar.getPlayers().contains(p)) bar.removePlayer(p);
        }
        if (!minions && boss.getHealth() < boss.getMaxHealth() * 0.5f) {
            minions = true;
            lvl.playSound(null, boss.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 2f, 0.6f);
            for (int i = 0; i < 3; i++) {
                double a = i * 2.1;
                Mobs.spawn(lvl, Mobs.Kind.ASH_ZOMBIE, 3, false, lair.add(Math.cos(a) * 6, 0, Math.sin(a) * 6), "floor1_mob", "");
            }
        }
        // землетрясение
        if (telegraph >= 0) {
            telegraph--;
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12;
                lvl.sendParticles(ParticleTypes.ANGRY_VILLAGER, boss.getX() + Math.cos(a) * 5.5, boss.getY() + 0.2, boss.getZ() + Math.sin(a) * 5.5, 1, 0, 0, 0, 0);
            }
            if (telegraph == 0) slam(lvl, boss);
        } else if (--abilityTimer <= 0) {
            abilityTimer = 140 + lvl.random.nextInt(60);
            telegraph = 25;
            lvl.playSound(null, boss.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2f, 0.7f);
        }
    }

    private static void slam(ServerLevel lvl, Mob boss) {
        lvl.playSound(null, boss.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5f, 0.6f);
        lvl.sendParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY() + 0.3, boss.getZ(), 8, 2.5, 0.2, 2.5, 0);
        lvl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, boss.getX(), boss.getY() + 0.2, boss.getZ(), 40, 3, 0.1, 3, 0.02);
        for (ServerPlayer p : lvl.players()) {
            if (p.distanceTo(boss) > 5.5 || !p.onGround() || p.isSpectator() || p.isCreative()) continue;
            p.hurt(lvl.damageSources().mobAttack(boss), 8f);
            Vec3 push = p.position().subtract(boss.position()).normalize().scale(0.9);
            p.setDeltaMovement(push.x, 0.75, push.z);
            p.hurtMarked = true;
        }
    }

    public static Mob spawn(ServerLevel lvl) {
        Zombie z = EntityType.ZOMBIE.create(lvl);
        if (z == null) return null;
        Vec3 c = Vec3.atBottomCenterOf(Floor1.LAIR);
        z.moveTo(c.x, c.y, c.z, 0, 0);
        Mobs.setup(z, LEVEL, false, "Гнилой Громила", "floor1_boss");
        double hp = Mobs.hp(LEVEL) * 10;
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        z.setHealth((float) hp);
        z.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Mobs.dmg(LEVEL) * 1.6);
        z.getAttribute(Attributes.SCALE).setBaseValue(1.9);
        z.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.22);
        z.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.8);
        z.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0);
        z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        z.setDropChance(EquipmentSlot.HEAD, 0);
        z.setCustomName(Component.literal("☠ [" + LEVEL + "] Гнилой Громила").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        z.setCustomNameVisible(true);
        z.addTag(TAG);
        lvl.addFreshEntity(z);
        bossId = z.getUUID();
        DAMAGE.clear();
        aliveTicks = 0; minions = false; telegraph = -1; abilityTimer = 120;
        bar = new ServerBossEvent(Component.literal("Гнилой Громила • ур. " + LEVEL), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
        lvl.playSound(null, Floor1.LAIR, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3f, 1.2f);
        lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 1, c.z, 120, 3, 2, 3, 0.05);
        broadcast(lvl.getServer(), "ГНИЛОЙ ГРОМИЛА ПРОСНУЛСЯ! Логово на юге этажа 1.", ChatFormatting.RED);
        return z;
    }

    public static void recordDamage(Mob mob, ServerPlayer p, float dmg) {
        if (isBoss(mob)) DAMAGE.merge(p.getUUID(), dmg, Float::sum);
    }

    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof Mob boss) || !isBoss(boss) || !(boss.level() instanceof ServerLevel lvl)) return;
        MinecraftServer server = lvl.getServer();
        float total = 0;
        for (float v : DAMAGE.values()) total += v;
        List<Map.Entry<UUID, Float>> sorted = new ArrayList<>(DAMAGE.entrySet());
        sorted.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        StringBuilder top = new StringBuilder();
        for (var en : sorted) {
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            float share = total > 0 ? en.getValue() / total : 0;
            String name = p != null ? p.getGameProfile().getName() : "?";
            if (top.length() < 120) top.append(name).append(" ").append(Math.round(share * 100)).append("%  ");
            if (p == null || share < 0.05f) continue;
            List<ItemStack> loot = LootTables.roll("floor1_boss_personal", lvl.random, 1.0);
            StoryData d = Progress.d(p);
            if ((d.trophies & 1) == 0) {
                d.trophies |= 1;
                p.setData(ModRegistry.STORY.get(), d);
                loot.add(new ItemStack(ModRegistry.BRUTE_TROPHY.get()));
            }
            for (ItemStack st : loot) {
                Story.announceRareDrop(lvl, st);
                com.pvpciv.inv.Bag.give(p, st);
            }
            p.sendSystemMessage(Component.literal("Личная награда за Громилу: " + loot.size() + " предметов (ты нанёс " + Math.round(share * 100) + "% урона)").withStyle(ChatFormatting.GOLD));
            Progress.addXp(p, Progress.xpFor(d.level, LEVEL, 12));
        }
        for (ItemStack st : LootTables.roll("floor1_boss_ground", lvl.random, 1.0)) {
            ItemEntity ie = new ItemEntity(lvl, boss.getX(), boss.getY() + 1, boss.getZ(), st);
            ie.setDeltaMovement((lvl.random.nextDouble() - 0.5) * 0.5, 0.4, (lvl.random.nextDouble() - 0.5) * 0.5);
            lvl.addFreshEntity(ie);
            Story.announceRareDrop(lvl, st);
        }
        lvl.playSound(null, boss.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 3f, 0.8f);
        broadcast(server, "Гнилой Громила повержен! " + top.toString().trim() + " — куча лута лежит в Логове, налетай!", ChatFormatting.GOLD);
    }

    private static void cleanup(MinecraftServer server, boolean killed) {
        if (bar != null) { bar.removeAllPlayers(); bar = null; }
        bossId = null;
        DAMAGE.clear();
        warned5 = warned1 = false;
        CivData data = CivData.get(server);
        data.bossNext = server.overworld().getGameTime() + PERIOD;
        data.setDirty();
    }

    /** Для тестов: спавн прямо сейчас. */
    public static void forceSpawn(MinecraftServer server) {
        if (bossId != null) return;
        CivData d = CivData.get(server);
        d.bossNext = server.overworld().getGameTime();
        d.setDirty();
    }

    @SuppressWarnings("unused") private static final DamageSource DS = null;
    @SuppressWarnings("unused") private static final BlockPos BP = BlockPos.ZERO;
}
