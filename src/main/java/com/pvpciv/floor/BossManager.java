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
    public static final Def KAPPA = new Def("kappa", "Братья-каппы", "Пруд деревни самураев, этаж 3", 13, 30 * MIN, 4.5, 1.45,
            () -> Floor3Areas.KAPPA_POND, 14, Vec3.ZERO, () -> com.pvpciv.registry.ModMobs.KAPPA_BOSS.get(),
            "floor3_boss_personal", "floor3_boss_ground", 16, ModRegistry.KAPPA_TROPHY, BossEvent.BossBarColor.GREEN, ChatFormatting.GREEN,
            "БРАТЬЯ-КАППЫ ВЫШЛИ ИЗ ПРУДА! Деревня самураев, этаж 3. Поклонитесь… или нет.", false);
    public static final Def KUMIHO = new Def("kumiho", "Кумихо", "Святилище в деревне Кумихо, этаж 3", 16, 40 * MIN, 14, 1.7,
            () -> Floor3Areas.KUMIHO_SHRINE, 13, new Vec3(0, 0, -4), () -> com.pvpciv.registry.ModMobs.KUMIHO_BOSS.get(),
            "floor3b_boss_personal", "floor3b_boss_ground", 32, ModRegistry.KUMIHO_TROPHY, BossEvent.BossBarColor.PINK, ChatFormatting.LIGHT_PURPLE,
            "КУМИХО ПРОБУДИЛАСЬ! Девять хвостов над деревней лис, этаж 3.", false);
    public static final Def NOEL = new Def("noel", "Доктор Ноэль и Лиана", "Биокупол «Ковчег», запад НТЦ NaZa (этаж 4)", 24, 45 * MIN, 9, 1.6,
            () -> Floor4.ARK_ARENA, 13, new Vec3(-7, 0, 0), () -> com.pvpciv.registry.ModMobs.NOEL_BOSS.get(),
            "floor4_boss_personal", "floor4_boss_ground", 64, com.pvpciv.registry.ModF4.NAZA_TROPHY, BossEvent.BossBarColor.GREEN, ChatFormatting.GREEN,
            "ДОКТОР НОЭЛЬ ВЫПУСТИЛ ЛИАНУ! Биокупол «Ковчег», этаж 4.", false);
    public static final Def DIPLOMAT = new Def("diplomat", "Инопланетный дипломат", "Мостик станции «Орбита» (этаж 5)", 32, 40 * MIN, 11, 1.5,
            () -> Floor5.BRIDGE_M.center(), 13, new Vec3(0, 0, -4), () -> com.pvpciv.registry.ModMobs.DIPLOMAT_BOSS.get(),
            "floor5_boss_personal", "floor5_boss_ground", 128, com.pvpciv.registry.ModF5.DIPLOMAT_TROPHY, BossEvent.BossBarColor.BLUE, ChatFormatting.AQUA,
            "ИНОПЛАНЕТНЫЙ ДИПЛОМАТ ПРИБЫЛ НА МОСТИК! Этаж 5. Не подписывайте ничего.", false);
    public static final Def WORM = new Def("worm", "Галактический червь", "Пояс астероидов (шлюз ангара, этаж 5)", 38, 60 * MIN, 13, 1.6,
            () -> Belt.CENTER, 30, new Vec3(0, 6, 0), () -> com.pvpciv.registry.ModMobs.WORM_BOSS.get(),
            "floor5_worm_personal", "floor5_worm_ground", 256, com.pvpciv.registry.ModF5.WORM_TROPHY, BossEvent.BossBarColor.PINK, ChatFormatting.LIGHT_PURPLE,
            "ГАЛАКТИЧЕСКИЙ ЧЕРВЬ ПРОСНУЛСЯ! Пояс астероидов, этаж 5.", false);
    public static final List<Def> DEFS = List.of(BRUTE, KEEPER, DON, RAT_KING, KAPPA, KUMIHO, NOEL, DIPLOMAT, WORM);
    public static boolean f5(Def d) { return d == DIPLOMAT || d == WORM; }
    /** Сколько тел у босса (каппы — четверо братьев, Ноэль + Лиана; урон и лут общие). */
    public static int group(Def d) { return d == KAPPA ? 4 : d == NOEL ? 2 : 1; }
    public static boolean f3(Def d) { return d == KAPPA || d == KUMIHO; }
    public static boolean f4(Def d) { return d == NOEL; }
    public static int floor(Def d) { return f5(d) ? 5 : f4(d) ? 4 : f3(d) ? 3 : d.f2() ? 2 : 1; }

    private static final class St {
        UUID id; ServerBossEvent bar; int alive; boolean w5, w1, scanned;
        /** 0.11.2: арена закрыта после первого удара; внутри — те, кто был там в этот момент. */
        boolean locked; final Set<UUID> inside = new HashSet<>();
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
        return (int) Math.max(0, (next - now()) / 20);
    }

    private static void broadcast(MinecraftServer s, String msg, ChatFormatting c) {
        for (ServerPlayer p : s.getPlayerList().getPlayers())
            p.sendSystemMessage(Component.literal("☠ ").withStyle(ChatFormatting.DARK_RED).append(Component.literal(msg).withStyle(c)));
    }

    private static Vec3 center(Def d) { return Vec3.atBottomCenterOf(d.lair().get()); }

    public static void tick(MinecraftServer server) {
        for (Def d : DEFS) tick(server, d);
    }

    /** Реальное время в тиках: таймеры боссов идут, даже когда сервер выключен (бета 12.11). */
    public static long now() { return System.currentTimeMillis() / 50; }

    private static ServerLevel lvl(MinecraftServer server, Def d) { if (f5(d)) return com.pvpciv.world.CivWorld.f5(server); if (f4(d)) return com.pvpciv.world.CivWorld.f4(server); if (f3(d)) return com.pvpciv.world.CivWorld.f3(server); return d.f2() ? com.pvpciv.world.CivWorld.f2(server) : com.pvpciv.world.CivWorld.level(server); }

    private static void tick(MinecraftServer server, Def d) {
        ServerLevel lvl = lvl(server, d);
        CivData data = CivData.get(server);
        St s = st(d);
        long now = now();
        long old = data.bossNext(d.id());
        if (old > 0 && old < 10_000_000_000L) { data.bossNext.put(d.id(), old - lvl.getGameTime() + now); data.setDirty(); }   // со старых игровых тиков
        BlockPos lairPos = d.lair().get();
        if (data.bossNext(d.id()) < 0) { data.bossNext.put(d.id(), now + 20L * 180); data.setDirty(); }
        boolean lairLoaded = lvl.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(lairPos));
        CivBoss boss = s.id == null ? null : (lvl.getEntity(s.id) instanceof CivBoss b ? b : null);
        // раз в 2 с сверяемся с миром: ровно один босс на логово (бета 0.8.7: «боссов двое»)
        if (!lairLoaded) s.scanned = false;
        boolean scan = lairLoaded && (server.getTickCount() % 40 == 0 || (s.id == null && !s.scanned));
        if (scan) {
            s.scanned = true;
            List<Mob> found = lvl.getEntitiesOfClass(Mob.class, new AABB(lairPos).inflate(d.lairR() + 60),
                    m -> m.getTags().contains(TAG) || (m instanceof CivBoss b && b.defId().equals(d.id())));
            if (boss == null) for (Mob m : found) if (m instanceof CivBoss b && b.isAlive()) { boss = b; break; }
            int keep = group(d) - 1;
            for (Mob m : found) if (m != boss) { if (keep > 0 && m instanceof CivBoss b && b.isAlive()) keep--; else m.discard(); }
            if (boss != null && s.id == null) { s.id = boss.getUUID(); s.bar = bar(d); }
        }
        if (s.id != null && (boss == null || !boss.isAlive()) && group(d) > 1 && lairLoaded) {   // каппы: живой брат становится «главным»
            for (CivBoss b : lvl.getEntitiesOfClass(CivBoss.class, new AABB(lairPos).inflate(d.lairR() + 60), b -> b.isAlive() && b.defId().equals(d.id()))) { boss = b; s.id = b.getUUID(); break; }
        }
        if (s.id != null && (boss == null || !boss.isAlive())) {
            if (boss == null && !lairLoaded) return;   // игроки ушли — босс ждёт в выгруженном чанке
            cleanup(server, d);
            return;
        }
        if (boss == null) {
            long left = data.bossNext(d.id()) - now;
            if (left <= 20L * 300 && !s.w5) { s.w5 = true; broadcast(server, "Алтарь «" + d.name() + "» оживёт через 5 минут: " + d.where() + " (PvP).", ChatFormatting.GOLD); }
            if (left <= 20L * 60 && !s.w1) { s.w1 = true; broadcast(server, "Алтарь «" + d.name() + "» оживёт через минуту.", d.chat()); }
            // 0.11.2: босс спит — по таймеру в логове загорается алтарь, будит его игрок (ПКМ по алтарю)
            if (left <= 0 && lairLoaded && s.scanned) altar(lvl, d, true);
            return;
        }
        Vec3 c = center(d);
        // без засыпаний: босс живёт, пока его не убьют; следующий — через фиксированное время после смерти
        if (boss.position().distanceTo(c) > d.lairR() + 6 || boss.getY() < c.y - 5) {
            lvl.sendParticles(ParticleTypes.LARGE_SMOKE, boss.getX(), boss.getY() + 1, boss.getZ(), 30, 0.6, 1, 0.6, 0.02);
            Vec3 sp = c.add(d.spawnOffset());
            boss.teleportTo(sp.x, sp.y, sp.z);
        }
        if (s.bar == null) s.bar = bar(d);
        if (d == NOEL) {   // 0.11: общая полоса из двух половин — справа Лиана (уходит первой), слева Ноэль
            float noel = 0, liana = 0;
            for (CivBoss b : lvl.getEntitiesOfClass(CivBoss.class, new AABB(lairPos).inflate(d.lairR() + 120), b -> b.isAlive() && b.defId().equals(d.id()))) {
                if (b != boss && (b.position().distanceTo(c) > d.lairR() + 6 || b.getY() < c.y - 5)) {   // оба держатся арены
                    Vec3 sp = b instanceof com.pvpciv.entity.boss.LianaBoss ? c : c.add(d.spawnOffset());
                    b.teleportTo(sp.x, sp.y, sp.z);
                }
                if (b instanceof com.pvpciv.entity.boss.LianaBoss) liana = b.getHealth() / b.getMaxHealth(); else noel = b.getHealth() / b.getMaxHealth();
            }
            s.bar.setProgress(Math.max(0, Math.min(1, 0.5f * noel + 0.5f * liana)));
        } else if (group(d) > 1) {
            float hp = 0, max = boss.getMaxHealth() * group(d);
            for (CivBoss b : lvl.getEntitiesOfClass(CivBoss.class, new AABB(lairPos).inflate(d.lairR() + 60), b -> b.isAlive() && b.defId().equals(d.id()))) hp += b.getHealth();
            s.bar.setProgress(Math.max(0, Math.min(1, hp / max)));
        } else s.bar.setProgress(Math.max(0, boss.getHealth() / boss.getMaxHealth()));
        s.bar.setName(Component.literal(d.name() + " • ур. " + d.level() + (boss.enraged() ? "  — ЯРОСТЬ" : "")));
        if (d == NOEL) s.bar.setName(Component.literal("Ноэль ▌ Лиана • ур. " + d.level() + (boss instanceof com.pvpciv.entity.boss.NoelBoss nb && nb.enraged() ? "  — ЯРОСТЬ" : "")));
        for (ServerPlayer p : new ArrayList<>(s.bar.getPlayers())) if (p.level() != lvl || p.isRemoved()) s.bar.removePlayer(p);   // 0.11: ушёл на другой этаж — полоса не висит
        lockTick(lvl, d, s, c);
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
        if (b instanceof com.pvpciv.entity.boss.KappaBoss k) {   // четверо братьев вокруг пруда
            k.season(0); k.pond = c;
            b.setCustomName(Component.literal("☠ [" + d.level() + "] " + com.pvpciv.entity.boss.KappaBoss.NAMES[0]).withStyle(d.chat(), ChatFormatting.BOLD));
            b.moveTo(c.x + 5, c.y, c.z, 180, 0);
            for (int i = 1; i < 4; i++) {
                CivBoss bb = d.type().get().create(lvl);
                if (!(bb instanceof com.pvpciv.entity.boss.KappaBoss kk)) continue;
                double a = Math.PI / 2 * i;
                Vec3 sp = new Vec3(c.x + Math.cos(a) * 5, c.y, c.z + Math.sin(a) * 5);
                kk.moveTo(sp.x, sp.y, sp.z, 180, 0);
                kk.setHome(c); kk.season(i); kk.pond = c;
                Mobs.setup(kk, d.level(), false, d.name(), d.personal());
                kk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
                kk.setHealth((float) hp);
                kk.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Mobs.dmg(d.level()) * d.dmgMul());
                kk.setCustomName(Component.literal("☠ [" + d.level() + "] " + com.pvpciv.entity.boss.KappaBoss.NAMES[i]).withStyle(d.chat(), ChatFormatting.BOLD));
                kk.setCustomNameVisible(true);
                lvl.addFreshEntity(kk);
                com.pvpciv.entity.FxEntity.spawn(lvl, com.pvpciv.entity.FxEntity.PILLAR, sp, 0.8f, 20, 0x5AAAFF, 0xE0F4FF);
            }
        }
        if (d == NOEL) {   // Лиана растёт в центре арены, Ноэль — сбоку
            CivBoss li = com.pvpciv.registry.ModMobs.LIANA_BOSS.get().create(lvl);
            if (li != null) {
                li.moveTo(c.x, c.y, c.z, 90, 0);
                li.setHome(c);
                Mobs.setup(li, d.level(), false, "Лиана", d.personal());
                double lhp = hp * 1.1;
                li.getAttribute(Attributes.MAX_HEALTH).setBaseValue(lhp);
                li.setHealth((float) lhp);
                li.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Mobs.dmg(d.level()) * d.dmgMul());
                li.setCustomName(Component.literal("☠ [" + d.level() + "] Лиана").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
                li.setCustomNameVisible(true);
                lvl.addFreshEntity(li);
            }
            b.setCustomName(Component.literal("☠ [" + d.level() + "] Доктор Ноэль").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        }
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
        if (mob instanceof CivBoss b) {
            Def d = def(b.defId());
            if (d == null) return;
            St s = st(d);
            s.dmg.merge(p.getUUID(), dmg, Float::sum);
            if (!s.locked && mob.level() instanceof ServerLevel lvl) lock(lvl, d, s);
        }
    }

    // ------------------------------------------------------------------ 0.11.2: алтарь пробуждения
    /** Блок алтаря стоит в центре логова, на земле. */
    private static final java.util.Map<String, BlockPos> ALTAR_AT = new java.util.HashMap<>();
    public static BlockPos altarPos(Def d) { return ALTAR_AT.getOrDefault(d.id(), d.lair().get()); }
    /** Ближайшая к центру логова сухая точка на земле (у капп центр — в пруду). */
    private static BlockPos findAltar(ServerLevel lvl, Def d) {
        BlockPos c = d.lair().get();
        for (int r = 0; r <= 8; r++) for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
            for (int dy = -2; dy <= 4; dy++) {
                BlockPos q = c.offset(dx, dy, dz);
                var st = lvl.getBlockState(q);
                if ((st.isAir() || st.is(net.minecraft.world.level.block.Blocks.RESPAWN_ANCHOR)) && lvl.getFluidState(q).isEmpty()
                        && lvl.getBlockState(q.below()).isFaceSturdy(lvl, q.below(), net.minecraft.core.Direction.UP) && lvl.getFluidState(q.below()).isEmpty()) return q;
            }
        }
        return c;
    }
    private static final java.util.Map<String, Boolean> ALTAR_LIT = new java.util.HashMap<>();

    private static void altar(ServerLevel lvl, Def d, boolean on) {
        BlockPos a = ALTAR_AT.computeIfAbsent(d.id(), k -> findAltar(lvl, d));
        boolean has = lvl.getBlockState(a).is(net.minecraft.world.level.block.Blocks.RESPAWN_ANCHOR);
        if (on && !has) {
            lvl.setBlock(a, net.minecraft.world.level.block.Blocks.RESPAWN_ANCHOR.defaultBlockState().setValue(net.minecraft.world.level.block.RespawnAnchorBlock.CHARGE, 4), 3);
            if (!Boolean.TRUE.equals(ALTAR_LIT.get(d.id()))) {
                broadcast(lvl.getServer(), "Алтарь «" + d.name() + "» горит: " + d.where() + ". ПКМ по алтарю — разбудить.", d.chat());
                lvl.playSound(null, a, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 2f, 0.6f);
            }
            ALTAR_LIT.put(d.id(), true);
        } else if (!on && has) lvl.setBlock(a, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        if (on && lvl.getGameTime() % 10 == 0)
            lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, a.getX() + 0.5, a.getY() + 1.2, a.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
    }

    /** ПКМ по блоку: если это горящий алтарь босса — будим. */
    public static boolean onAltarUse(ServerPlayer p, BlockPos pos) {
        for (Def d : DEFS) {
            if (!altarPos(d).equals(pos) || lvl(p.server, d) != p.level()) continue;
            if (!p.level().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.RESPAWN_ANCHOR)) return false;
            St s = st(d);
            if (s.id != null || secondsLeft(p.server, d.id()) > 0) return true;
            altar((ServerLevel) p.level(), d, false);
            ALTAR_LIT.remove(d.id());
            broadcast(p.server, p.getGameProfile().getName() + " будит: " + d.name() + "!", d.chat());
            spawn((ServerLevel) p.level(), d);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ 0.11.2: закрытая арена
    private static void lock(ServerLevel lvl, Def d, St s) {
        Vec3 c = center(d);
        s.locked = true;
        s.inside.clear();
        for (ServerPlayer p : lvl.players()) if (!p.isSpectator() && flat(p.position(), c) <= d.lairR()) s.inside.add(p.getUUID());
        lvl.playSound(null, BlockPos.containing(c), SoundEvents.IRON_DOOR_CLOSE, SoundSource.HOSTILE, 3f, 0.5f);
        lvl.playSound(null, BlockPos.containing(c), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2f, 0.6f);
        for (ServerPlayer p : lvl.players()) if (flat(p.position(), c) < d.lairR() + 30)
            p.displayClientMessage(Component.literal("Арена закрыта до конца боя").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
    }

    private static double flat(Vec3 a, Vec3 b) { return Math.hypot(a.x - b.x, a.z - b.z); }

    /** Каждый тик боя: стена-кольцо из частиц; своих не выпускает, чужих не пускает. Все свои ушли или погибли — арена открывается. */
    private static void lockTick(ServerLevel lvl, Def d, St s, Vec3 c) {
        if (!s.locked) return;
        double R = d.lairR();
        s.inside.removeIf(u -> { ServerPlayer p = lvl.getServer().getPlayerList().getPlayer(u); return p == null || !p.isAlive() || p.level() != lvl; });
        if (s.inside.isEmpty()) { s.locked = false; return; }
        if (lvl.getGameTime() % 6 == 0) for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2 * i / 48 + lvl.getGameTime() * 0.01;
            lvl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x + Math.cos(a) * R, c.y + 0.2 + (i % 4) * 0.9, c.z + Math.sin(a) * R, 1, 0, 0.2, 0, 0);
        }
        for (ServerPlayer p : lvl.players()) {
            if (p.isSpectator() || p.isCreative()) continue;
            double r = flat(p.position(), c);
            boolean in = s.inside.contains(p.getUUID());
            if (in && r > R + 8) { s.inside.remove(p.getUUID()); continue; }   // ушёл телепортом (врата, лифт) — выпускаем
            if (in && r > R - 0.6) push(p, c, -1, R - 1.2);
            else if (!in && r < R + 0.6 && r > 0.01) push(p, c, 1, R + 1.2);
        }
    }

    /** dir −1 — к центру (вернуть на радиус), +1 — наружу. */
    private static void push(ServerPlayer p, Vec3 c, int dir, double toR) {
        Vec3 v = new Vec3(p.getX() - c.x, 0, p.getZ() - c.z).normalize();
        Vec3 at = new Vec3(c.x + v.x * toR, p.getY(), c.z + v.z * toR);
        p.teleportTo(at.x, at.y, at.z);
        p.setDeltaMovement(v.scale(0.35 * dir).add(0, 0.1, 0));
        p.hurtMarked = true;
        if (p.tickCount % 20 == 0) p.displayClientMessage(Component.literal(dir < 0 ? "Арена закрыта — бой до конца" : "Арена закрыта: бой уже идёт").withStyle(ChatFormatting.RED), true);
    }

    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof CivBoss boss) || !(boss.level() instanceof ServerLevel lvl)) return;
        Def d = def(boss.defId());
        if (d == null) return;
        St s = st(d);
        if (group(d) > 1) {   // каппы: награда — когда пал последний брат
            List<CivBoss> rest = lvl.getEntitiesOfClass(CivBoss.class, boss.getBoundingBox().inflate(d.lairR() + 60), b -> b != boss && b.isAlive() && b.defId().equals(d.id()));
            if (!rest.isEmpty()) {
                for (CivBoss b : rest) if (b instanceof com.pvpciv.entity.boss.KappaBoss k) k.brotherDown(lvl);
 if (d == NOEL) broadcast(lvl.getServer(), "Лиана срезана! Щит доктора Ноэля пал.", ChatFormatting.GREEN); else
                broadcast(lvl.getServer(), boss.getCustomName() == null ? "Брат-каппа пал!" : boss.getCustomName().getString().replace("☠ ", "") + " пал! Осталось: " + rest.size(), ChatFormatting.GREEN);
                if (s.id != null && s.id.equals(boss.getUUID())) s.id = rest.get(0).getUUID();
                return;
            }
        }
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
            LootTables.PREFER = com.pvpciv.game.Classes.prefer(p);
            List<ItemStack> loot;
            try { loot = LootTables.roll(d.personal(), lvl.random, 1.0); } finally { LootTables.PREFER = null; }
            StoryData sd = Progress.d(p);
            if ((sd.trophies & d.trophyBit()) == 0) {
                sd.trophies |= d.trophyBit();
                p.setData(ModRegistry.STORY.get(), sd);
                loot.add(new ItemStack(d.trophy().get()));
            }
            for (ItemStack st : loot) {
                Story.announceRareDrop(lvl, st, p.getGameProfile().getName());
                com.pvpciv.inv.Bag.give(p, st);
            }
            p.sendSystemMessage(Component.literal("Личная награда («" + d.name() + "»): " + loot.size() + " предметов (ты нанёс " + Math.round(share * 100) + "% урона)").withStyle(ChatFormatting.GOLD));
            Progress.addXp(p, Progress.xpFor(sd.level, d.level(), 12));
            com.pvpciv.game.Marks.onBossKill(p, d.id());
            com.pvpciv.game.Library.onKillBoss(p, d.id());
            com.pvpciv.game.Assassin.onKill(p, com.pvpciv.game.Assassin.BOSS);
            if (d == NOEL) { com.pvpciv.game.Enchanter.onBoss(p); com.pvpciv.progress.Titles.grant(p, "gardener"); }
            if (d == DIPLOMAT) com.pvpciv.progress.Titles.grant(p, "ambassador");
        }
        if (d == WORM) com.pvpciv.game.VoidRift.begin(lvl, new ArrayList<>(s.dmg.keySet()));   // 1.0: фаза 2 — демоница Пустоты
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
        s.locked = false; s.inside.clear();
        CivData data = CivData.get(server);
        data.bossNext.put(d.id(), now() + d.period());
        data.setDirty();
    }

    /** Для тестов: босс проснётся прямо сейчас (как только рядом будет игрок). */
    public static boolean forceSpawn(MinecraftServer server, String id) {
        Def d = def(id);
        if (d == null || st(d).id != null) return false;
        CivData data = CivData.get(server);
        data.bossNext.put(id, now());
        data.setDirty();
        return true;
    }
}
