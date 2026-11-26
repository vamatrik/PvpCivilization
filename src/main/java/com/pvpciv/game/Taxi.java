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
            // 1.2: этажи 3–5
            fl(3, "Тихая библиотека", 0, com.pvpciv.floor.Floor3.Y, 6, 0),
            fl(3, "Деревня самураев", com.pvpciv.floor.Floor3Areas.SV.getX(), com.pvpciv.floor.Floor3.Y, com.pvpciv.floor.Floor3Areas.SV.getZ(), 1),
            fl(3, "Пруд капп", com.pvpciv.floor.Floor3Areas.KAPPA_POND.getX() - 12, com.pvpciv.floor.Floor3.Y, com.pvpciv.floor.Floor3Areas.KAPPA_POND.getZ(), 3),
            fl(3, "Деревня Кумихо", com.pvpciv.floor.Floor3Areas.KV.getX(), com.pvpciv.floor.Floor3.Y, com.pvpciv.floor.Floor3Areas.KV.getZ(), 2),
            fl(3, "Святилище Кумихо", com.pvpciv.floor.Floor3Areas.KUMIHO_SHRINE.getX() + 14, com.pvpciv.floor.Floor3.Y, com.pvpciv.floor.Floor3Areas.KUMIHO_SHRINE.getZ(), 3),
            fl(4, "Вестибюль NaZa", 0, com.pvpciv.floor.Floor4.Y, 6, 0),
            fl(4, "Оранжерея", com.pvpciv.floor.Floor4.GREEN.getX(), com.pvpciv.floor.Floor4.Y, com.pvpciv.floor.Floor4.GREEN.getZ() + 8, 1),
            fl(4, "Криолаборатория", com.pvpciv.floor.Floor4.CRYO.getX() - 8, com.pvpciv.floor.Floor4.Y, com.pvpciv.floor.Floor4.CRYO.getZ(), 1),
            fl(4, "Реакторный купол", com.pvpciv.floor.Floor4.REACTOR.getX(), com.pvpciv.floor.Floor4.Y, com.pvpciv.floor.Floor4.REACTOR.getZ() - 8, 2),
            fl(4, "Биокупол «Ковчег»", com.pvpciv.floor.Floor4.ARK.getX() + 8, com.pvpciv.floor.Floor4.Y, com.pvpciv.floor.Floor4.ARK.getZ(), 2),
            fl(4, "Космодром", com.pvpciv.floor.Floor4.COSMO.getX(), com.pvpciv.floor.Floor4.Y, com.pvpciv.floor.Floor4.COSMO.getZ() - 6, 1),
            fl(5, "Атриум", com.pvpciv.floor.Floor5.HUB.center().getX(), com.pvpciv.floor.Floor5.Y, com.pvpciv.floor.Floor5.HUB.center().getZ() + 6, 0),
            fl(5, "Ангар", com.pvpciv.floor.Floor5.HANGAR_M.center().getX(), com.pvpciv.floor.Floor5.Y, com.pvpciv.floor.Floor5.HANGAR_M.center().getZ() - 8, 1),
            f5("Гидропоника", com.pvpciv.floor.Floor5.Type.HYDRO, 2),
            f5("Обсерватория", com.pvpciv.floor.Floor5.Type.OBSERVATORY, 2),
            f5("Медотсек", com.pvpciv.floor.Floor5.Type.MEDBAY, 2),
            fl(5, "Мостик", com.pvpciv.floor.Floor5.BRIDGE_M.center().getX(), com.pvpciv.floor.Floor5.Y, com.pvpciv.floor.Floor5.BRIDGE_M.center().getZ() + 12, 3),
    };

    /** 1.2: первый модуль станции такого типа (если нет — атриум). */
    private static Pt f5(String n, com.pvpciv.floor.Floor5.Type t, int deep) {
        com.pvpciv.floor.Floor5.Module m = com.pvpciv.floor.Floor5.HUB;
        for (var q : com.pvpciv.floor.Floor5.MODULES) if (q.type() == t) { m = q; break; }
        return fl(5, n, m.center().getX(), com.pvpciv.floor.Floor5.Y, m.center().getZ() + 6, deep);
    }

    /** 1.2: точка этажей 3–5 (схема — по рамке всех точек этажа). */
    private static Pt fl(int floor, String n, int x, int y, int z, int deep) { return new Pt(n, floor, x, y, z, deep, 0, 0); }

    /** Позиция точки на схеме 0..1 (этажи 3–5 — нормировка по рамке точек этажа, поля 10%). */
    public static float[] mapPos(Pt p) {
        if (p.floor <= 2) return new float[]{p.mx, p.my};
        int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z0 = Integer.MAX_VALUE, z1 = Integer.MIN_VALUE;
        for (Pt q : PTS) if (q.floor == p.floor) { x0 = Math.min(x0, q.x); x1 = Math.max(x1, q.x); z0 = Math.min(z0, q.z); z1 = Math.max(z1, q.z); }
        float w = Math.max(1, x1 - x0), h = Math.max(1, z1 - z0);
        return new float[]{0.1f + 0.8f * (p.x - x0) / w, 0.1f + 0.8f * (p.z - z0) / h};
    }

    public static final String[] FLOOR_NAMES = {"", "Пустошь", "Неон-сити", "Деревни", "НТЦ «NaZa»", "«Орбита»"};

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
        if (a.floor == b.floor) return Math.min(900, Math.hypot(a.x - b.x, a.z - b.z)) + Math.abs(a.y - b.y) * 2;
        return Math.min(450, Math.hypot(a.x, a.z)) + Math.min(450, Math.hypot(b.x, b.z));          // через лифт в центре каждого этажа
    }

    /** Общая формула (клиент показывает ту же цену). */
    public static int price(int from, int to) {
        if (from == to || from < 0 || to < 0 || from >= PTS.length || to >= PTS.length) return 0;
        Pt a = PTS[from], b = PTS[to];
        return 25 + (int) Math.round(dist(a, b) * 0.3) + b.deep * 70 * Math.max(1, b.floor - 1) + (a.floor != b.floor ? LIFT_FEE * Math.max(1, Math.abs(a.floor - b.floor)) : 0);
    }

    /** from = -1 — забрать «отсюда» (at), to = -2 — домой в квартиру. */
    private record Order(int from, int to, int price, long until, Vec3 at) {
        Order(int from, int to, int price, long until) { this(from, to, price, until, null); }
    }

    /** Домой: подача + немного за расстояние, с этажа 2 — плюс лифт. */
    public static int homePrice(ServerPlayer p) {
        com.pvpciv.data.CivData.Apartment a = com.pvpciv.data.CivData.get(p.server).byId(Progress.d(p).apartment);
        if (a == null) return -1;
        int fl = CivWorld.floor(p.level());
        if (fl >= 3) return 20 + 20 * fl + LIFT_FEE * (fl - 1);
        if (CivWorld.isF2(p.level())) return 40 + LIFT_FEE / 2;
        return 30 + (int) Math.min(60, Math.hypot(p.getX() - a.door.getX(), p.getZ() - a.door.getZ()) / 10);
    }

    public static void home(ServerPlayer p) {
        int price = homePrice(p);
        StoryData d = Progress.d(p);
        if (price < 0) { msg(p, "У тебя нет квартиры.", ChatFormatting.RED); return; }
        if (Vampire.jailed(p)) { msg(p, "Из темницы такси не вызвать. Попробуй «Выкуп».", ChatFormatting.RED); return; }
        if (PENDING.containsKey(p.getUUID())) { msg(p, "Одно такси уже едет к тебе.", ChatFormatting.RED); return; }
        if (d.money < price) { msg(p, "Не хватает Искр: " + price + " ✦.", ChatFormatting.RED); return; }
        d.money -= price;
        p.setData(ModRegistry.STORY.get(), d);
        Story.sync(p);
        PENDING.put(p.getUUID(), new Order(-1, -2, price, p.server.getTickCount() + WAIT_TICKS, p.position()));
        Phone.chime(p);
        msg(p, "🚕 Такси домой подъезжает: стой на месте, " + price + " ✦.", ChatFormatting.YELLOW);
    }
    private static final Map<UUID, Order> PENDING = new HashMap<>();

    /** Ближайшая точка к игроку (в пределах PICKUP_R) или -1. */
    public static int nearest(ServerPlayer p) {
        int fl = CivWorld.floor(p.level());
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
        j.addProperty("open", SkyLift.unlocked(d));   // 1.2: какие этажи открыты (биты)
        j.addProperty("here", nearest(p));
        j.addProperty("floor", Math.max(1, CivWorld.floor(p.level())));
        j.addProperty("homePrice", homePrice(p));
        if (o != null) { j.addProperty("pf", o.from); j.addProperty("pt", o.to); }
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(2, j.toString()));
    }

    public static void order(ServerPlayer p, int from, int to) {
        StoryData d = Progress.d(p);
        if (from < 0 || to < 0 || from >= PTS.length || to >= PTS.length || from == to) return;
        if ((PTS[from].floor == 2 || PTS[to].floor == 2) && !d.liftOk) { msg(p, "Таксист: «На второй этаж без пропуска Луны не повезу».", ChatFormatting.RED); return; }
        int open = SkyLift.unlocked(d);
        if ((open & (1 << (PTS[from].floor - 1))) == 0 || (open & (1 << (PTS[to].floor - 1))) == 0) { msg(p, "Таксист: «Туда лифт тебя ещё не пускает — сначала открой этаж».", ChatFormatting.RED); return; }
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
        boolean here;
        String fromName;
        if (o.from < 0) { here = p.position().distanceToSqr(o.at) < 36; fromName = "здесь"; }
        else {
            Pt a = PTS[o.from];
            here = a.floor == CivWorld.floor(p.level())
                    && p.distanceToSqr(a.x + 0.5, a.y, a.z + 0.5) < PICKUP_R * PICKUP_R;
            fromName = a.name;
        }
        com.google.gson.JsonObject j = new com.google.gson.JsonObject();
        j.addProperty("name", fromName); j.addProperty("to", o.to < 0 ? "домой" : PTS[o.to].name); j.addProperty("sec", (o.until - now) / 20); j.addProperty("here", here);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(5, j.toString()));
        if (!here || p.isPassenger() || Vampire.jailed(p) || Gates.inCombat(p)) return;
        PENDING.remove(p.getUUID());
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(5, "{}"));
        if (o.to == -2) rideHome(p); else ride(p, o.to);
    }

    private static void rideHome(ServerPlayer p) {
        com.pvpciv.data.CivData.Apartment a = com.pvpciv.data.CivData.get(p.server).byId(Progress.d(p).apartment);
        if (a == null) return;
        PacketDistributor.sendToPlayer(p, new Network.Cinematic(6, 90, "Такси\nДомой"));
        GameEvents.later(p.server, 4, () -> {
            if (!p.isAlive() || p.hasDisconnected()) return;
            p.teleportTo(CivWorld.level(p.server), a.spawn.getX() + 0.5, a.spawn.getY(), a.spawn.getZ() + 0.5, a.yaw, 0);
            p.resetFallDistance();
            msg(p, "🚕 Приехали домой. Поставьте пять звёзд водителю!", ChatFormatting.YELLOW);
        });
    }

    private static void ride(ServerPlayer p, int to) {
        Pt b = PTS[to];
        ServerLevel lvl = switch (b.floor) { case 2 -> CivWorld.f2(p.server); case 3 -> CivWorld.f3(p.server); case 4 -> CivWorld.f4(p.server); case 5 -> CivWorld.f5(p.server); default -> CivWorld.level(p.server); };
        Vec3 at = safe(lvl, new BlockPos(b.x, b.y, b.z));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.MINECART_RIDING, SoundSource.PLAYERS, 0.6f, 1.3f);
        PacketDistributor.sendToPlayer(p, new Network.Cinematic(6, 90, "Такси\n" + b.name));
        GameEvents.later(p.server, 4, () -> {
            if (!p.isAlive() || p.hasDisconnected()) return;
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
