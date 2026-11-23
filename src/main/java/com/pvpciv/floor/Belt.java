package com.pvpciv.floor;

import com.pvpciv.game.Story;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.world.CivWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/**
 * 1.0 — пояс астероидов (этаж 5, к югу от станции). Попасть — шлюз ангара (ключ: трофей Дипломата), кат-сцена выхода.
 * Низкая гравитация. В центре — большой астероид с алтарём Галактического червя, вокруг — поле астероидов-ступенек:
 * червь их глотает (прыгай с них!), через 25 с они возвращаются. Сорвался в пустоту — назад на шлюзовую площадку.
 */
public final class Belt {
    private Belt() {}
    public static final int Y = 110;
    public static final BlockPos CENTER = new BlockPos(0, Y, 760), PAD = new BlockPos(0, Y, 812);
    public static final int FIELD = 28;

    public record Rock(int id, BlockPos c, int r) {}
    public static final List<Rock> ROCKS = new ArrayList<>();
    static {
        RandomSource r = RandomSource.create(7007);
        int id = 0;
        // кольца ступенек вокруг центра, соседние — в прыжке при низкой гравитации
        // 1.1.1: два кольца вместо трёх (было 33 камня — «съеденный» камень терялся среди остальных)
        for (int ring = 0; ring < 2; ring++) {
            int n = 6 + ring * 3;
            double rad = 12 + ring * 8;
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * (i + (ring % 2) * 0.5) / n + r.nextDouble() * 0.15;
                int x = CENTER.getX() + (int) Math.round(Math.cos(a) * rad), z = CENTER.getZ() + (int) Math.round(Math.sin(a) * rad);
                int y = Y - 2 + r.nextInt(6);
                ROCKS.add(new Rock(id++, new BlockPos(x, y, z), 2 + r.nextInt(2)));
            }
        }
        // мостик-ступеньки от площадки к полю
        for (int i = 0; i < 4; i++) ROCKS.add(new Rock(id++, new BlockPos(CENTER.getX() + (i % 2 == 0 ? -1 : 2), Y - 1 + i % 2, PAD.getZ() - 8 - i * 5), 2));
    }

    private static final Map<Integer, Long> GONE = new HashMap<>();
    public static boolean gone(int id) { return GONE.containsKey(id); }

    public static boolean inBelt(net.minecraft.world.level.Level l, Vec3 p) {
        return CivWorld.isF5(l) && Math.abs(p.x - CENTER.getX()) < 140 && p.z > CENTER.getZ() - 140 && p.z < PAD.getZ() + 80;
    }

    // ------------------------------------------------------------------ постройка
    public static void build(ServerLevel l) {
        for (Rock k : ROCKS) rock(l, k, true);
        center(l, true);
        // шлюзовая площадка
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
            double d = Math.hypot(x, z);
            if (d > 6.4) continue;
            l.setBlock(PAD.offset(x, -1, z), d > 5.5 ? Blocks.YELLOW_CONCRETE.defaultBlockState() : (x + z) % 3 == 0 ? Blocks.IRON_BLOCK.defaultBlockState() : Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), 2);
            l.setBlock(PAD.offset(x, -2, z), Blocks.GRAY_CONCRETE.defaultBlockState(), 2);
        }
        for (int[] o : new int[][]{{-5, -3}, {5, -3}, {-5, 3}, {5, 3}}) for (int y = 0; y < 4; y++) l.setBlock(PAD.offset(o[0], y, o[1]), y == 3 ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.IRON_BARS.defaultBlockState(), 2);
        BlockPos k = CONSOLE();
        l.setBlock(k, com.pvpciv.block.ConsoleBlock.of(com.pvpciv.block.ConsoleBlock.Kind.STATION, Direction.NORTH), 2);
        sign(l, k.above(2), "ШЛЮЗ «ОРБИТА»", "ПКМ по пульту —", "назад в ангар", "");
        sign(l, PAD.offset(0, 1, -6), "ПОЯС АСТЕРОИДОВ", "низкая гравитация", "червь — в центре", "(алтарь, ПКМ)");
    }

    public static BlockPos CONSOLE() { return PAD.offset(3, 0, 4); }

    /** Центральный астероид: широкая плоская вершина (on=false — разбит). */
    public static void center(ServerLevel l, boolean on) {
        RandomSource r = RandomSource.create(77);
        for (int x = -9; x <= 9; x++) for (int z = -9; z <= 9; z++) for (int y = -7; y <= 0; y++) {
            double d = Math.sqrt(x * x + z * z + (y * 1.6) * (y * 1.6));
            if (d > 8.5 + r.nextDouble() * 0.8) continue;
            BlockState st = y == 0 ? top(r, x, z) : stone(r);
            BlockPos q = CENTER.offset(x, y - 1, z);
            if (on) l.setBlock(q, st, 2);
            else if (!l.getBlockState(q).is(com.pvpciv.registry.ModRegistry.BOSS_ALTAR.get())) l.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
        }
    }
    public static final int CENTER_ID = -1;
    public static boolean centerGone() { return GONE.containsKey(CENTER_ID); }

    /** 1.1.1: червь разбил центральный астероид — вернётся через 25 с или сразу, когда червь на него ляжет. */
    public static void breakCenter(ServerLevel l) {
        if (centerGone()) return;
        GONE.put(CENTER_ID, l.getGameTime() + 500);
        center(l, false);
        l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, CENTER.getX() + 0.5, CENTER.getY(), CENTER.getZ() + 0.5, 3, 4, 1, 4, 0);
    }
    public static void restoreCenter(ServerLevel l) {
        if (GONE.remove(CENTER_ID) == null) return;
        center(l, true);
        l.sendParticles(ParticleTypes.END_ROD, CENTER.getX() + 0.5, CENTER.getY(), CENTER.getZ() + 0.5, 80, 6, 0.5, 6, 0.02);
    }

    /** 1.1.1: старое поле (3 кольца, 33 камня) — убрать всё между центром и площадкой, затем построить заново. */
    public static void rebuildField(ServerLevel l) {
        for (int x = -40; x <= 40; x++) for (int z = -40; z <= 46; z++) {
            double h = Math.hypot(x, z);
            if (h < 10.5 || h > 47) continue;
            for (int y = -8; y <= 7; y++) {
                BlockPos q = CENTER.offset(x, y, z);
                if (!l.getBlockState(q).isAir()) l.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        for (Rock k : ROCKS) rock(l, k, true);
        // площадку не трогаем (она дальше 47 от центра)
    }

    private static void sign(ServerLevel l, BlockPos at, String... lines) {
        l.setBlock(at.below(), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 2);
        com.pvpciv.world.Signs.place(l, at, Direction.SOUTH, 5, true, net.minecraft.world.item.DyeColor.LIGHT_BLUE.getTextColor(), lines);
        if (false && l.getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.SignBlockEntity s) {
            var text = s.getFrontText();
            for (int i = 0; i < 4 && i < lines.length; i++) text = text.setMessage(i, Component.literal(lines[i]));
            s.setText(text.setColor(net.minecraft.world.item.DyeColor.LIGHT_BLUE).setHasGlowingText(true), true);
            s.setText(text, false);
            s.setChanged();
        }
    }

    private static BlockState stone(RandomSource r) {
        int k = r.nextInt(12);
        return (k < 5 ? Blocks.BLACKSTONE : k < 8 ? Blocks.DEEPSLATE : k < 10 ? Blocks.TUFF : k == 10 ? Blocks.AMETHYST_BLOCK : Blocks.CRYING_OBSIDIAN).defaultBlockState();
    }
    private static BlockState top(RandomSource r, int x, int z) {
        int k = r.nextInt(10);
        return (k < 6 ? Blocks.POLISHED_BLACKSTONE : k < 9 ? Blocks.COBBLED_DEEPSLATE : Blocks.AMETHYST_BLOCK).defaultBlockState();
    }

    /** Астероид-ступенька: плоский верх (на нём стоят), снизу — каменный «киль». */
    private static void rock(ServerLevel l, Rock k, boolean on) {
        RandomSource r = RandomSource.create(k.id * 31L + 5);
        for (int x = -k.r - 1; x <= k.r + 1; x++) for (int z = -k.r - 1; z <= k.r + 1; z++) for (int y = -k.r - 1; y <= 0; y++) {
            double d = Math.sqrt(x * x + z * z + (y * 1.3) * (y * 1.3));
            if (d > k.r + 0.6) continue;
            BlockPos q = k.c.offset(x, y - 1, z);
            l.setBlock(q, on ? (y == 0 ? top(r, x, z) : stone(r)) : Blocks.AIR.defaultBlockState(), 2);
        }
    }

    /** Червь проглотил астероид: камень исчезает, через 25 с возвращается. */
    public static void swallow(ServerLevel l, Rock k) {
        GONE.put(k.id, l.getGameTime() + 500);
        l.sendParticles(ParticleTypes.EXPLOSION, k.c.getX() + 0.5, k.c.getY(), k.c.getZ() + 0.5, 4, k.r, 0.5, k.r, 0);
        l.sendParticles(ParticleTypes.REVERSE_PORTAL, k.c.getX() + 0.5, k.c.getY(), k.c.getZ() + 0.5, 60, k.r, 1, k.r, 0.3);
        rock(l, k, false);
    }

    // ------------------------------------------------------------------ шлюз
    /** ПКМ по пульту шлюза в ангаре или на площадке. */
    public static boolean onUse(ServerPlayer p, BlockPos pos) {
        boolean out = CivWorld.isF5(p.level()) && pos.equals(Floor5.AIRLOCK());
        boolean back = CivWorld.isF5(p.level()) && pos.equals(CONSOLE());
        if (!out && !back) return false;
        if (EXITING.containsKey(p.getUUID())) return true;
        if (out && (Progress.d(p).trophies & 128) == 0) {
            Story.say(p, "Шлюз", 0x40D0FF, "ВЫХОД ЗАПРЕЩЁН. Разрешение подписывает посол. (Победи Инопланетного дипломата на Мостике.)");
            return true;
        }
        if (out) {   // 1.1.1: свой шлюз с анимацией вылета
            if (!com.pvpciv.game.Airlock.start(p)) p.displayClientMessage(Component.literal("Шлюз уже открыт — подожди следующего цикла.").withStyle(ChatFormatting.AQUA), true);
            return true;
        }
        EXITING.put(p.getUUID(), out ? 1 : -1);
        TICKS.put(p.getUUID(), 0);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(22, "{\"cut\":\"airlock\",\"len\":60}"));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 2f, 0.5f);
        p.displayClientMessage(Component.literal(out ? "Шлюз открывается… разгерметизация" : "Шлюз: возвращение в ангар").withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    /** Прибытие на площадку после вылета из шлюза. */
    public static void arriveOut(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        p.teleportTo(l, PAD.getX() + 0.5, Y, PAD.getZ() + 0.5, 180, 0);
        p.resetFallDistance();
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(22, "{\"cut\":\"arrive\",\"len\":30}"));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("Пояс астероидов").withStyle(ChatFormatting.LIGHT_PURPLE)));
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal("Низкая гравитация · ур. 38").withStyle(ChatFormatting.GRAY)));
        if (!Progress.d(p).gates.contains("belt_seen")) {
            var d = Progress.d(p); d.gates.putInt("belt_seen", 1); p.setData(com.pvpciv.registry.ModRegistry.STORY.get(), d);
            p.sendSystemMessage(Component.literal("✦ Пояс астероидов. Прыжки тут длинные, падать — далеко. В центре на большом астероиде — алтарь Галактического червя.").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
    private static final Map<UUID, Integer> EXITING = new HashMap<>(), TICKS = new HashMap<>();

    // ------------------------------------------------------------------ ход
    private static final ResourceLocation GRAV = com.pvpciv.PvpCiv.id("low_gravity");

    public static void tick(MinecraftServer server) {
        ServerLevel l = CivWorld.f5(server);
        if (!CivWorld.isF5(l)) return;
        // астероиды возвращаются
        if (!GONE.isEmpty()) {
            long now = l.getGameTime();
            GONE.entrySet().removeIf(en -> {
                if (en.getValue() > now) return false;
                if (en.getKey() == CENTER_ID) { center(l, true); return true; }
                for (Rock k : ROCKS) if (k.id == en.getKey()) { rock(l, k, true); l.sendParticles(ParticleTypes.END_ROD, k.c.getX() + 0.5, k.c.getY(), k.c.getZ() + 0.5, 20, k.r, 0.5, k.r, 0.02); }
                return true;
            });
        }
        // шлюз
        EXITING.entrySet().removeIf(en -> {
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            if (p == null) return true;
            int t = TICKS.merge(en.getKey(), 1, Integer::sum);
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 10, 10, false, false));
            if (t % 10 == 0) l.playSound(null, p.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1f, 0.5f);
            if (t < 45) return false;
            if (en.getValue() > 0) {
                p.teleportTo(l, PAD.getX() + 0.5, Y, PAD.getZ() + 0.5, 180, 0);
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("Пояс астероидов").withStyle(ChatFormatting.LIGHT_PURPLE)));
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.literal("Низкая гравитация · ур. 38").withStyle(ChatFormatting.GRAY)));
                if (!Progress.d(p).gates.contains("belt_seen")) {
                    var d = Progress.d(p); d.gates.putInt("belt_seen", 1); p.setData(com.pvpciv.registry.ModRegistry.STORY.get(), d);
                    p.sendSystemMessage(Component.literal("✦ Пояс астероидов. Прыжки тут длинные, падать — далеко. В центре на большом астероиде — алтарь Галактического червя.").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            } else {
                Vec3 a = Floor5.HANGAR_ARRIVE();
                p.teleportTo(l, a.x, a.y, a.z, 180, 0);
            }
            p.resetFallDistance();
            PacketDistributor.sendToPlayer(p, new Network.PhoneData(22, "{\"cut\":\"arrive\",\"len\":30}"));
            TICKS.remove(en.getKey());
            return true;
        });
        // низкая гравитация
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            var g = p.getAttribute(Attributes.GRAVITY);
            if (g == null) continue;
            boolean in = p.level() == l && inBelt(l, p.position());
            boolean on = g.getModifier(GRAV) != null;
            if (in && !on) g.addTransientModifier(new AttributeModifier(GRAV, -0.62, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            else if (!in && on) g.removeModifier(GRAV);
            if (in) p.fallDistance = Math.min(p.fallDistance, 2f);
        }
    }

    /** Падение в пустоту пояса (вызов из Falling): назад на площадку. true — обработано. */
    public static boolean fall(ServerPlayer p) {
        if (!inBelt(p.level(), p.position()) || p.getY() > Y - 30) return false;
        p.teleportTo(PAD.getX() + 0.5, Y, PAD.getZ() + 0.5);
        p.resetFallDistance();
        p.hurt(p.damageSources().fellOutOfWorld(), p.getMaxHealth() * 0.25f);
        p.sendSystemMessage(Component.literal("Тебя подобрал дрон шлюза. Скафандр помят: −25% здоровья.").withStyle(ChatFormatting.RED));
        return true;
    }

    @SuppressWarnings("unused") private static final Direction D = Direction.NORTH;
    @SuppressWarnings("unused") private static final Block B = Blocks.AIR;
}
