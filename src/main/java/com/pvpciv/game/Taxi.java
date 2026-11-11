package com.pvpciv.game;

import com.pvpciv.data.StoryData;
import com.pvpciv.floor.Floor1;
import com.pvpciv.floor.Floor2;
import com.pvpciv.floor.FloorNorth;
import com.pvpciv.floor.Zones;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.CivWorld;
import com.pvpciv.world.DormBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Такси «Шпиль.Такси»: схема этажей, точки посадки и высадки из списка.
 * Цена = подача + расстояние + «наценка за глушь» точки назначения. До пристани, от которой идёт трасса, — дёшево;
 * сразу к логову босса — дорого. Между этажами — плюс плата за лифт.
 * Если точка посадки далеко, таксист ждёт там 3 минуты.
 */
public final class Taxi {
    /** floor: 1 или 2; x,z — мировые координаты; deep — наценка (0..3); mx,my — место на схеме (0..1). */
    public record Pt(String name, int floor, int x, int y, int z, int deep, float mx, float my) {}

    public static final Pt[] PTS = {
            f1("Общежитие", 0, DormBuilder.Y0, 8, 0),
            f1("Пристань Тракта", DormBuilder.DOCK_CENTER.getX(), DormBuilder.Y0, DormBuilder.DOCK_CENTER.getZ() + 2, 0),
            f1("Лагерь Пустоши", Floor1.CAMP.getX(), Floor1.Y, Floor1.CAMP.getZ() + 4, 1),
            f1("Руины", Floor1.RUINS.getX(), Floor1.Y, Floor1.RUINS.getZ(), 2),
            f1("Арена Пустоши", Floor1.ARENA.getX(), Floor1.Y, Floor1.ARENA.getZ() - 12, 2),
            f1("Логово Громилы", Floor1.LAIR.getX(), Floor1.Y, Floor1.LAIR.getZ() - Floor1.LAIR_R - 6, 3),
            f1("Северная пристань", DormBuilder.NORTH_DOCK.getX(), DormBuilder.Y0, DormBuilder.NORTH_DOCK.getZ() - 1, 0),
            f1("Аванпост Кряжа", FloorNorth.OUTPOST.getX(), FloorNorth.Y, FloorNorth.OUTPOST.getZ() - 4, 1),
            f1("Ледяное озеро", FloorNorth.LAKE.getX(), FloorNorth.Y, FloorNorth.LAKE.getZ(), 2),
            new Pt("Станция лифта", 2, (int) Floor2.LIFT_ARRIVE.x, Floor2.Y, (int) Floor2.LIFT_ARRIVE.z + 2, 0, 0.5f, 0.5f),
            pol("Площадь Неона", 45, 120, Floor2.Y, 1),
            pol("Рынок", 135, 120, Floor2.Y, 1),
            pol("Парк", 225, 120, Floor2.Y, 1),
            pol("Клуб «Серра»", 315, 120 - Floor2.CLUB_R - 6, Floor2.Y, 3),
            pol("Небоскрёб «Альтаир»", 15, 74, Floor2.Y, 1),
            pol("Сантехник Гоша", 0, 124, Floor2.YS, 2),
    };

    /** Этаж 1 вытянут с севера на юг: на схеме z идёт слева направо. */
    private static Pt f1(String n, int x, int y, int z, int deep) {
        return new Pt(n, 1, x, y, z, deep, f1x(z), 0.5f + x / 240f);
    }

    /** Схема этажа 1: z → 0..1, около общежития растянуто (там три точки рядом). */
    public static float f1x(int z) {
        float k = (float) Math.pow(Math.min(1, Math.abs(z) / 700f), 0.55);
        return 0.5f + Math.signum(z) * k * 0.48f;
    }

    private static Pt pol(String n, double deg, double r, int y, int deep) {
        BlockPos b = Floor2.polar(deg, r, y);
        double a = Math.toRadians(deg);
        return new Pt(n, 2, b.getX(), y, b.getZ(), deep, (float) (0.5 + Math.cos(a) * r / 300.0), (float) (0.5 + Math.sin(a) * r / 300.0));
    }

    public static final int LIFT_FEE = 200, WAIT_TICKS = 20 * 180, PICKUP_R = 24;

    private static double dist(Pt a, Pt b) {
        if (a.floor == b.floor) return Math.hypot(a.x - b.x, a.z - b.z) + Math.abs(a.y - b.y) * 2;
        return Math.hypot(a.x, a.z) + Math.hypot(b.x, b.z);          // через лифт в центре каждого этажа
    }

    /** Общая формула (клиент показывает ту же цену). */
    public static int price(int from, int to) {
        if (from == to || from < 0 || to < 0 || from >= PTS.length || to >= PTS.length) return 0;
        Pt a = PTS[from], b = PTS[to];
        return 25 + (int) Math.round(dist(a, b) * 0.3) + b.deep * 70 + (a.floor != b.floor ? LIFT_FEE : 0);
    }

    private record Order(int from, int to, int price, long until) {}
    private static final Map<UUID, Order> PENDING = new HashMap<>();

    /** Ближайшая точка к игроку (в пределах PICKUP_R) или -1. */
    public static int nearest(ServerPlayer p) {
        int fl = CivWorld.isF2(p.level()) ? 2 : p.level().dimension() == CivWorld.WORLD1 ? 1 : 0;
        int best = -1; double bd = PICKUP_R * PICKUP_R;
        for (int i = 0; i < PTS.length; i++) {
            Pt t = PTS[i];
            if (t.floor != fl) continue;
            double d = p.distanceToSqr(t.x + 0.5, t.y, t.z + 0.5);
            if (d < bd) { bd = d; best = i; }
        }
        return best;
    }

    public static void open(ServerPlayer p) {
        StoryData d = Progress.d(p);
        Order o = PENDING.get(p.getUUID());
        com.google.gson.JsonObject j = new com.google.gson.JsonObject();
        j.addProperty("f2", d.liftOk);
        j.addProperty("here", nearest(p));
        j.addProperty("floor", CivWorld.isF2(p.level()) ? 2 : 1);
        if (o != null) { j.addProperty("pf", o.from); j.addProperty("pt", o.to); }
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(2, j.toString()));
    }

    public static void order(ServerPlayer p, int from, int to) {
        StoryData d = Progress.d(p);
        if (from < 0 || to < 0 || from >= PTS.length || to >= PTS.length || from == to) return;
        if ((PTS[from].floor == 2 || PTS[to].floor == 2) && !d.liftOk) { msg(p, "Таксист: «На второй этаж без пропуска Луны не повезу».", ChatFormatting.RED); return; }
        if (Vampire.jailed(p)) { msg(p, "Из темницы такси не вызвать. Попробуй «Выкуп».", ChatFormatting.RED); return; }
        if (PENDING.containsKey(p.getUUID())) { msg(p, "Одно такси уже едет к тебе. Отмени заказ в приложении.", ChatFormatting.RED); return; }
        int price = price(from, to);
        if (d.money < price) { msg(p, "Не хватает Искр: поездка стоит " + price + " ✦.", ChatFormatting.RED); return; }
        d.money -= price;
        p.setData(ModRegistry.STORY.get(), d);
        Story.sync(p);
        PENDING.put(p.getUUID(), new Order(from, to, price, p.server.getTickCount() + WAIT_TICKS));
        Phone.chime(p);
        if (from == nearest(p)) msg(p, "🚕 Такси подъезжает. Стой на месте — «" + PTS[to].name + "», " + price + " ✦.", ChatFormatting.YELLOW);
        else msg(p, "🚕 Такси ждёт у точки «" + PTS[from].name + "» 3 минуты. Едем: «" + PTS[to].name + "», " + price + " ✦.", ChatFormatting.YELLOW);
    }

    public static void cancel(ServerPlayer p) {
        Order o = PENDING.remove(p.getUUID());
        if (o == null) return;
        int back = Math.max(0, o.price - 20);
        Progress.addMoney(p, back, false);
        msg(p, "🚕 Заказ отменён. Вернули " + back + " ✦ (20 ✦ — за подачу).", ChatFormatting.GRAY);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(5, "{}"));
    }

    /** Раз в 10 тиков: таксист ждёт на точке посадки. */
    public static void tick(ServerPlayer p) {
        Order o = PENDING.get(p.getUUID());
        if (o == null) return;
        long now = p.server.getTickCount();
        if (now > o.until) {
            PENDING.remove(p.getUUID());
            int back = o.price / 2;
            Progress.addMoney(p, back, false);
            msg(p, "🚕 Таксист не дождался и уехал. Вернул половину: " + back + " ✦.", ChatFormatting.GRAY);
            PacketDistributor.sendToPlayer(p, new Network.PhoneData(5, "{}"));
            return;
        }
        Pt a = PTS[o.from];
        boolean here = a.floor == (CivWorld.isF2(p.level()) ? 2 : p.level().dimension() == CivWorld.WORLD1 ? 1 : 0)
                && p.distanceToSqr(a.x + 0.5, a.y, a.z + 0.5) < PICKUP_R * PICKUP_R;
        com.google.gson.JsonObject j = new com.google.gson.JsonObject();
        j.addProperty("name", a.name); j.addProperty("to", PTS[o.to].name); j.addProperty("sec", (o.until - now) / 20); j.addProperty("here", here);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(5, j.toString()));
        if (!here || p.isPassenger() || Vampire.jailed(p) || p.getLastHurtByMobTimestamp() > p.tickCount - 100) return;
        PENDING.remove(p.getUUID());
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(5, "{}"));
        ride(p, o.to);
    }

    private static void ride(ServerPlayer p, int to) {
        Pt b = PTS[to];
        ServerLevel lvl = b.floor == 2 ? CivWorld.f2(p.server) : CivWorld.level(p.server);
        Vec3 at = safe(lvl, new BlockPos(b.x, b.y, b.z));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.MINECART_RIDING, SoundSource.PLAYERS, 0.6f, 1.3f);
        PacketDistributor.sendToPlayer(p, new Network.Cinematic(6, 90, "Такси\n" + b.name));
        GameEvents.later(p.server, 4, () -> {
            if (!p.isAlive()) return;
            float yaw = (float) Math.toDegrees(Math.atan2(-(0 - at.x), (0 - at.z)));
            p.teleportTo(lvl, at.x, at.y, at.z, b.floor == 1 && b.z > 300 ? 180 : yaw, 0);
            p.resetFallDistance();
            msg(p, "🚕 Приехали: «" + b.name + "». Поставьте пять звёзд водителю!", ChatFormatting.YELLOW);
        });
    }

    /** Ближайшее место, где можно встать: твёрдый пол, два блока воздуха, не лава и не вода. */
    public static Vec3 safe(ServerLevel l, BlockPos c) {
        for (int r = 0; r <= 8; r++)
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                for (int dy : new int[]{0, 1, -1, 2, -2, 3, 4, -3}) {
                    BlockPos f = c.offset(dx, dy, dz);
                    var below = l.getBlockState(f.below());
                    if (!below.isFaceSturdy(l, f.below(), net.minecraft.core.Direction.UP) || below.is(Blocks.LAVA) || below.is(Blocks.MAGMA_BLOCK)) continue;
                    if (!l.getBlockState(f).getCollisionShape(l, f).isEmpty() || !l.getBlockState(f.above()).getCollisionShape(l, f.above()).isEmpty()) continue;
                    if (!l.getFluidState(f).isEmpty()) continue;
                    return new Vec3(f.getX() + 0.5, f.getY(), f.getZ() + 0.5);
                }
            }
        return new Vec3(c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5);
    }

    public static void forget(ServerPlayer p) { PENDING.remove(p.getUUID()); }

    private static void msg(ServerPlayer p, String s, ChatFormatting c) { p.sendSystemMessage(Component.literal(s).withStyle(c)); }
}
