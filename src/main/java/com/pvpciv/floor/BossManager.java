package com.pvpciv.floor;

import com.pvpciv.combat.Mobs;
import com.pvpciv.data.CivData;
import com.pvpciv.data.StoryData;
import com.pvpciv.entity.boss.CivBoss;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.*;
import java.util.function.Supplier;

/**
 * Боссы этажей. Каждый — {@link Def}: логово, уровень, период, таблицы лута, бит трофея.
 * Босс просыпается по таймеру (пока у логова никого нет — ждёт), держится у логова, живёт 30 мин;
 * полоса здоровья видна тем, кто рядом; личная награда — тем, кто нанёс ≥5% урона, общий лут падает на землю.
 */
public final class BossManager {
    public static final String TAG = "pvpciv_boss";
    public static final long MIN = 20L * 60;

    public record Def(String id, String name, String where, int level, long period, double hpMul, double dmgMul,
                      Supplier<BlockPos> lair, int lairR, Vec3 spawnOffset, Supplier<? extends EntityType<? extends CivBoss>> type,
                      String personal, String ground, int trophyBit, Supplier<? extends Item> trophy,
                      BossEvent.BossBarColor color, ChatFormatting chat, String wakeMsg, boolean f2) {}

    public static final Def BRUTE = new Def("brute", "Гнилой Громила", "Логово на юге Пепельной пустоши", 5, 20 * MIN, 10, 1.6,
            () -> Floor1.LAIR, Floor1.LAIR_R, Vec3.ZERO, () -> com.pvpciv.registry.ModMobs.BRUTE_BOSS.get(),
            "floor1_boss_personal", "floor1_boss_ground", 1, ModRegistry.BRUTE_TROPHY, BossEvent.BossBarColor.RED, ChatFormatting.RED,
            "ГНИЛОЙ ГРОМИЛА ПРОСНУЛСЯ! Логово на юге этажа 1.", false);
    public static final Def KEEPER = new Def("keeper", "Хранительница Склепа", "Ледяной Склеп на севере Кряжа", 8, 40 * MIN, 12, 1.5,
            () -> FloorNorth.CRYPT, FloorNorth.CRYPT_R, new Vec3(0, 0, -4), () -> com.pvpciv.registry.ModMobs.KEEPER_BOSS.get(),
            "floor1b_boss_personal", "floor1b_boss_ground", 2, ModRegistry.KEEPER_TROPHY, BossEvent.BossBarColor.BLUE, ChatFormatting.AQUA,
            "ХРАНИТЕЛЬНИЦА СКЛЕПА ПРОБУДИЛАСЬ! Ледяной Склеп на севере Кряжа.", false);
    public static final Def DON = new Def("don", "Дон Витторио", "Клуб «Серра», юг Неонового города", 10, 25 * MIN, 12, 1.6,
            () -> Floor2.CLUB, Floor2.CLUB_R, Vec3.ZERO, () -> com.pvpciv.registry.ModMobs.DON_BOSS.get(),
            "floor2_boss_personal", "floor2_boss_ground", 4, ModRegistry.DON_TROPHY, BossEvent.BossBarColor.YELLOW, ChatFormatting.GOLD,
            "ДОН ВИТТОРИО В ДЕЛЕ! Клуб «Серра» на юге Неонового города.", true);
    public static final Def RAT_KING = new Def("rat_king", "Крысиный король", "Тронный сток, канализация этажа 2", 13, 45 * MIN, 14, 1.5,
            () -> Floor2.RAT_THRONE, Floor2.RAT_R, Vec3.ZERO, () -> com.pvpciv.registry.ModMobs.RAT_KING_BOSS.get(),
            "floor2b_boss_personal", "floor2b_boss_ground", 8, ModRegistry.RAT_KING_TROPHY, BossEvent.BossBarColor.PURPLE, ChatFormatting.DARK_PURPLE,
            "КРЫСИНЫЙ КОРОЛЬ ВЫПОЛЗ НА ТРОН! Канализация этажа 2.", true);
    public static final List<Def> DEFS = List.of(BRUTE, KEEPER, DON, RAT_KING);

    private static final class St {
        UUID id; ServerBossEvent bar; int alive; boolean w5, w1;
        final Map<UUID, Float> dmg = new HashMap<>();
    }
    private static final Map<String, St> ST = new HashMap<>();
    private static St st(Def d) { return ST.computeIfAbsent(d.id(), k -> new St()); }

    public static Def def(String id) { for (Def d : DEFS) if (d.id().equals(id)) return d; return null; }

    public static boolean isBoss(LivingEntity e) { return e instanceof CivBoss || e.getTags().contains(TAG); }

    /** Секунд до спавна Громилы (-1 если жив). */
    public static int secondsLeft(MinecraftServer server) { return secondsLeft(server, "brute"); }

    public static int secondsLeft(MinecraftServer server, String id) {
        Def d = def(id);
        if (d == null || st(d).id != null) return -1;
        long next = CivData.get(server).bossNext(id);
        return (int) Math.max(0, (next - com.pvpciv.world.CivWorld.level(server).getGameTime()) / 20);
    }

    private static void broadcast(MinecraftServer s, String msg, ChatFormatting c) {
        for (ServerPlayer p : s.getPlayerList().getPlayers())
            p.sendSystemMessage(Component.literal("☠ ").withStyle(ChatFormatting.DARK_RED).append(Component.literal(msg).withStyle(c)));
    }

    private static Vec3 center(Def d) { return Vec3.atBottomCenterOf(d.lair().get()); }

    public static void tick(MinecraftServer server) {
        for (Def d : DEFS) tick(server, d);
    }

    private static ServerLevel lvl(MinecraftServer server, Def d) { return d.f2() ? com.pvpciv.world.CivWorld.f2(server) : com.pvpciv.world.CivWorld.level(server); }

    private static void tick(MinecraftServer server, Def d) {
        ServerLevel lvl = lvl(server, d);
        CivData data = CivData.get(server);
        St s = st(d);
        long now = lvl.getGameTime();
        BlockPos lairPos = d.lair().get();
        if (data.bossNext(d.id()) < 0) { data.bossNext.put(d.id(), now + 20L * 180); data.setDirty(); }
        boolean lairLoaded = lvl.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(lairPos));
        CivBoss boss = s.id == null ? null : (lvl.getEntity(s.id) instanceof CivBoss b ? b : null);
        if (s.id == null && lairLoaded && now % 40 == 0) {
            // подхватываем босса, пережившего перезапуск; старого Громилу-зомби (до 0.7) убираем
            List<Mob> found = lvl.getEntitiesOfClass(Mob.class, new AABB(lairPos).inflate(40),
                    m -> m.getTags().contains(TAG) || (m instanceof CivBoss b && b.defId().equals(d.id())));
            for (Mob m : found) {
                if (m instanceof CivBoss b && boss == null) boss = b;
                else m.discard();
            }
            if (boss != null) { s.id = boss.getUUID(); s.bar = bar(d); }
        }
        if (s.id != null && (boss == null || !boss.isAlive())) {
            if (boss == null && !lairLoaded) return;   // игроки ушли — босс ждёт в выгруженном чанке
            cleanup(server, d);
            return;
        }
        if (boss == null) {
            long left = data.bossNext(d.id()) - now;
            if (left <= 20L * 300 && !s.w5) { s.w5 = true; broadcast(server, d.name() + " проснётся через 5 минут: " + d.where() + " (PvP)!", ChatFormatting.GOLD); }
            if (left <= 20L * 60 && !s.w1) { s.w1 = true; broadcast(server, d.name() + " проснётся через минуту! Готовьте оружие.", d.chat()); }
            if (left <= 0 && lairLoaded) spawn(lvl, d);
            return;
        }
        if (++s.alive > 20 * 60 * 30) {
            broadcast(server, d.name() + " теряет интерес и засыпает. Вернётся позже.", ChatFormatting.GRAY);
            boss.discard();
            cleanup(server, d);
            return;
        }
        Vec3 c = center(d);
        if (boss.position().distanceTo(c) > d.lairR() + 6) {
            lvl.sendParticles(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY() + 1, boss.getZ(), 30, 0.6, 1, 0.6, 0.02);
            Vec3 sp = c.add(d.spawnOffset());
            boss.teleportTo(sp.x, sp.y, sp.z);
        }
        if (s.bar == null) s.bar = bar(d);
        s.bar.setProgress(Math.max(0, boss.getHealth() / boss.getMaxHealth()));
        s.bar.setName(Component.literal(d.name() + " • ур. " + d.level() + (boss.enraged() ? "  — ЯРОСТЬ" : "")));
        for (ServerPlayer p : lvl.players()) {
            boolean near = p.position().distanceTo(c) < d.lairR() + 34;
            if (near && !s.bar.getPlayers().contains(p)) s.bar.addPlayer(p);
            if (!near && s.bar.getPlayers().contains(p)) s.bar.removePlayer(p);
        }
    }

    private static ServerBossEvent bar(Def d) {
        return new ServerBossEvent(Component.literal(d.name() + " • ур. " + d.level()), d.color(), BossEvent.BossBarOverlay.NOTCHED_10);
    }

    public static CivBoss spawn(ServerLevel lvl, Def d) {
        CivBoss b = d.type().get().create(lvl);
        if (b == null) return null;
        St s = st(d);
        Vec3 c = center(d), at = c.add(d.spawnOffset());
        b.moveTo(at.x, at.y, at.z, 180, 0);
        b.setHome(c);
        Mobs.setup(b, d.level(), false, d.name(), d.personal());
        double hp = Mobs.hp(d.level()) * d.hpMul();
        b.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        b.setHealth((float) hp);
        b.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Mobs.dmg(d.level()) * d.dmgMul());
        b.setCustomName(Component.literal("☠ [" + d.level() + "] " + d.name()).withStyle(d.chat(), ChatFormatting.BOLD));
        b.setCustomNameVisible(true);
        b.setGlowingTag(false);
        lvl.addFreshEntity(b);
        s.id = b.getUUID();
        s.dmg.clear();
        s.alive = 0;
        if (s.bar != null) s.bar.removeAllPlayers();
        s.bar = bar(d);
        lvl.playSound(null, BlockPos.containing(at), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3f, d == KEEPER ? 1.6f : 1.2f);
        lvl.sendParticles(d == KEEPER ? ParticleTypes.SNOWFLAKE : ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 1, at.z, 120, 3, 2, 3, 0.05);
        broadcast(lvl.getServer(), d.wakeMsg(), d.chat());
        return b;
    }

    public static void recordDamage(Mob mob, ServerPlayer p, float dmg) {
        if (mob instanceof CivBoss b) { Def d = def(b.defId()); if (d != null) st(d).dmg.merge(p.getUUID(), dmg, Float::sum); }
    }

    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof CivBoss boss) || !(boss.level() instanceof ServerLevel lvl)) return;
        Def d = def(boss.defId());
        if (d == null) return;
        St s = st(d);
        MinecraftServer server = lvl.getServer();
        float total = 0;
        for (float v : s.dmg.values()) total += v;
        List<Map.Entry<UUID, Float>> sorted = new ArrayList<>(s.dmg.entrySet());
        sorted.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        StringBuilder top = new StringBuilder();
        for (var en : sorted) {
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            float share = total > 0 ? en.getValue() / total : 0;
            String name = p != null ? p.getGameProfile().getName() : "?";
            if (top.length() < 120) top.append(name).append(" ").append(Math.round(share * 100)).append("%  ");
            if (p == null || share < 0.05f) continue;
            List<ItemStack> loot = LootTables.roll(d.personal(), lvl.random, 1.0);
            StoryData sd = Progress.d(p);
            if ((sd.trophies & d.trophyBit()) == 0) {
                sd.trophies |= d.trophyBit();
                p.setData(ModRegistry.STORY.get(), sd);
                loot.add(new ItemStack(d.trophy().get()));
            }
            for (ItemStack st : loot) {
                Story.announceRareDrop(lvl, st);
                com.pvpciv.inv.Bag.give(p, st);
            }
            p.sendSystemMessage(Component.literal("Личная награда («" + d.name() + "»): " + loot.size() + " предметов (ты нанёс " + Math.round(share * 100) + "% урона)").withStyle(ChatFormatting.GOLD));
            Progress.addXp(p, Progress.xpFor(sd.level, d.level(), 12));
        }
        for (ItemStack st : LootTables.roll(d.ground(), lvl.random, 1.0)) {
            ItemEntity ie = new ItemEntity(lvl, boss.getX(), boss.getY() + 1, boss.getZ(), st);
            ie.setDeltaMovement((lvl.random.nextDouble() - 0.5) * 0.5, 0.4, (lvl.random.nextDouble() - 0.5) * 0.5);
            lvl.addFreshEntity(ie);
            Story.announceRareDrop(lvl, st);
        }
        lvl.playSound(null, boss.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 3f, 0.8f);
        broadcast(server, d.name() + (d == KEEPER ? " повержена! " : " повержен! ") + top.toString().trim() + " — лут лежит у логова, налетай!", ChatFormatting.GOLD);
    }

    private static void cleanup(MinecraftServer server, Def d) {
        St s = st(d);
        if (s.bar != null) { s.bar.removeAllPlayers(); s.bar = null; }
        s.id = null;
        s.dmg.clear();
        s.w5 = s.w1 = false;
        CivData data = CivData.get(server);
        data.bossNext.put(d.id(), com.pvpciv.world.CivWorld.level(server).getGameTime() + d.period());
        data.setDirty();
    }

    /** Для тестов: босс проснётся прямо сейчас (как только рядом будет игрок). */
    public static boolean forceSpawn(MinecraftServer server, String id) {
        Def d = def(id);
        if (d == null || st(d).id != null) return false;
        CivData data = CivData.get(server);
        data.bossNext.put(id, com.pvpciv.world.CivWorld.level(server).getGameTime());
        data.setDirty();
        return true;
    }
}
