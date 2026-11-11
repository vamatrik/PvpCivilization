package com.pvpciv.game;

import com.pvpciv.data.CivData;
import com.pvpciv.data.StoryData;
import com.pvpciv.entity.CityNpcEntity;
import com.pvpciv.inv.Bag;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.CivWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Supplier;

import static com.pvpciv.game.DialogueManager.opt;

/**
 * «Шпиль.Еда» — доставка в квартиру. Дороже, чем у торговцев; на складе мало; доставка платная и может опоздать.
 * Курьер с жёлтым рюкзаком ждёт у двери квартиры 3 минуты — забирать заказ нужно самому.
 */
public final class FoodApp {
    public record Dish(String id, String name, Supplier<Item> item, int count, int price, int stock) {}

    public static final Dish[] MENU = {
            new Dish("stew", "Похлёбка", () -> ModRegistry.DORM_STEW.get(), 1, 25, 8),
            new Dish("bread", "Хлеб, 2 шт.", () -> Items.BREAD, 2, 18, 6),
            new Dish("beef", "Стейк, 2 шт.", () -> Items.COOKED_BEEF, 2, 55, 6),
            new Dish("pie", "Пирог с тыквой", () -> Items.PUMPKIN_PIE, 1, 30, 5),
            new Dish("honey", "Мёд", () -> Items.HONEY_BOTTLE, 1, 40, 3),
            new Dish("carrot", "Золотая морковь", () -> Items.GOLDEN_CARROT, 1, 70, 4),
            new Dish("apple", "Золотое яблоко", () -> Items.GOLDEN_APPLE, 1, 750, 1),
    };
    /** Тарифы: обычная / экспресс. */
    public static final int[] FEE = {40, 120};
    public static final int[] ETA_S = {90, 40};
    public static final float[] LATE = {0.35f, 0.10f};
    public static final long RESTOCK_MS = 30 * 60_000L, WAIT_MS = 3 * 60_000L;

    private static long now() { return System.currentTimeMillis(); }
    public static long restockIn() { return RESTOCK_MS - now() % RESTOCK_MS; }

    private static CompoundTag stock(StoryData d) {
        long rot = now() / RESTOCK_MS;
        if (d.foodStock.getLong("rot") != rot) { d.foodStock = new CompoundTag(); d.foodStock.putLong("rot", rot); }
        return d.foodStock;
    }

    public static int left(StoryData d, int i) { return MENU[i].stock - stock(d).getInt(MENU[i].id); }

    public static void open(ServerPlayer p) {
        StoryData d = Progress.d(p);
        com.google.gson.JsonObject j = new com.google.gson.JsonObject();
        com.google.gson.JsonArray st = new com.google.gson.JsonArray();
        for (int i = 0; i < MENU.length; i++) st.add(left(d, i));
        j.add("stock", st);
        j.addProperty("restock", restockIn() / 1000);
        j.addProperty("money", d.money);
        j.addProperty("apt", apt(p) == null ? -1 : apt(p).number);
        int state = d.food.getInt("state");
        j.addProperty("state", state);
        if (state == 1) j.addProperty("eta", Math.max(0, (d.food.getLong("eta") - now()) / 1000));
        if (state == 2) j.addProperty("wait", Math.max(0, (d.food.getLong("door") + WAIT_MS - now()) / 1000));
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(1, j.toString()));
    }

    private static CivData.Apartment apt(ServerPlayer p) { return CivData.get(p.server).byId(Progress.d(p).apartment); }

    /** cart: "индекс*кол-во,..." ; tariff 0/1. */
    public static void order(ServerPlayer p, String cart, int tariff) {
        StoryData d = Progress.d(p);
        if (tariff < 0 || tariff > 1) return;
        if (d.food.getInt("state") != 0) { msg(p, "Один заказ уже в пути.", ChatFormatting.RED); return; }
        CivData.Apartment a = apt(p);
        if (a == null) { msg(p, "Нет адреса доставки: сначала заселись в общежитие.", ChatFormatting.RED); return; }
        int[] q = new int[MENU.length];
        int total = 0, n = 0;
        for (String part : cart.split(",")) {
            String[] kv = part.split("\\*");
            if (kv.length != 2) continue;
            int i, c;
            try { i = Integer.parseInt(kv[0]); c = Integer.parseInt(kv[1]); } catch (NumberFormatException e) { return; }
            if (i < 0 || i >= MENU.length || c <= 0) continue;
            if (c > left(d, i)) { msg(p, "«" + MENU[i].name + "» закончился на складе.", ChatFormatting.RED); return; }
            q[i] = c; total += c * MENU[i].price; n += c;
        }
        if (n == 0) return;
        total += FEE[tariff];
        if (d.money < total) { msg(p, "Не хватает Искр: заказ стоит " + total + " ✦.", ChatFormatting.RED); return; }
        d.money -= total;
        CompoundTag s = stock(d);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MENU.length; i++) if (q[i] > 0) { s.putInt(MENU[i].id, s.getInt(MENU[i].id) + q[i]); sb.append(i).append(':').append(q[i]).append(','); }
        boolean late = p.getRandom().nextFloat() < LATE[tariff];
        long eta = now() + ETA_S[tariff] * 1000L + (late ? (tariff == 0 ? 45 + p.getRandom().nextInt(105) : 20 + p.getRandom().nextInt(40)) * 1000L : 0);
        CompoundTag f = new CompoundTag();
        f.putInt("state", 1); f.putString("cart", sb.toString()); f.putInt("fee", FEE[tariff]); f.putInt("sum", total - FEE[tariff]);
        f.putLong("eta", eta); f.putLong("promised", now() + ETA_S[tariff] * 1000L); f.putBoolean("late", late); f.putInt("apt", a.id);
        d.food = f;
        p.setData(ModRegistry.STORY.get(), d);
        Story.sync(p);
        Phone.chime(p);
        msg(p, "🛵 «Шпиль.Еда»: заказ принят, " + total + " ✦. Курьер привезёт к двери кв. " + a.number + " через ~" + ETA_S[tariff] + " с.", ChatFormatting.GREEN);
    }

    /** Раз в секунду. */
    public static void tick(ServerPlayer p) {
        StoryData d = Progress.d(p);
        int state = d.food.getInt("state");
        if (state == 0) return;
        long t = now();
        CivData.Apartment a = CivData.get(p.server).byId(d.food.getInt("apt"));
        if (a == null) { d.food = new CompoundTag(); return; }
        if (state == 1) {
            if (t < d.food.getLong("eta")) {
                if (d.food.getBoolean("late") && !d.food.getBoolean("warned") && t > d.food.getLong("promised")) {
                    d.food.putBoolean("warned", true);
                    msg(p, "🛵 «Шпиль.Еда»: курьер задерживается. Пробки на лифте. Мы очень извиняемся.", ChatFormatting.GRAY);
                }
                return;
            }
            d.food.putInt("state", 2);
            d.food.putLong("door", t);
            p.setData(ModRegistry.STORY.get(), d);
            Phone.chime(p);
            msg(p, "🛵 Курьер у двери кв. " + a.number + "! Ждёт 3 минуты — выйди и забери заказ.", ChatFormatting.GREEN);
            return;
        }
        ServerLevel lvl = CivWorld.level(p.server);
        BlockPos spot = spot(a);
        CityNpcEntity c = courier(lvl, spot, p);
        if (t > d.food.getLong("door") + WAIT_MS && !DialogueManager.inDialogue(p)) {
            int back = d.food.getInt("sum");
            d.food = new CompoundTag();
            p.setData(ModRegistry.STORY.get(), d);
            Progress.addMoney(p, back, false);
            if (c != null) c.discard();
            msg(p, "🛵 Курьер не дождался и уехал. За еду вернули " + back + " ✦, доставка не возвращается.", ChatFormatting.GRAY);
            return;
        }
        if (c == null && lvl.areEntitiesLoaded(ChunkPos.asLong(spot))) {
            c = ModRegistry.COURIER.get().create(lvl);
            if (c == null) return;
            c.owner = p.getUUID();
            c.home = new net.minecraft.world.phys.Vec3(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
            c.moveTo(c.home.x, c.home.y, c.home.z, p.getRandom().nextFloat() * 360, 0);
            lvl.addFreshEntity(c);
        }
    }

    private static CityNpcEntity courier(ServerLevel l, BlockPos spot, ServerPlayer p) {
        if (!l.areEntitiesLoaded(ChunkPos.asLong(spot))) return null;
        for (CityNpcEntity e : l.getEntitiesOfClass(CityNpcEntity.class, new AABB(spot).inflate(16), e -> e.role == CityNpcEntity.Role.COURIER))
            if (p.getUUID().equals(e.owner)) return e;
        return null;
    }

    /** Место курьера: галерея перед дверью квартиры. */
    public static BlockPos spot(CivData.Apartment a) {
        int ix = Integer.signum(a.sign.getX() - a.door.getX()), iz = Integer.signum(a.sign.getZ() - a.door.getZ());
        return a.door.offset(ix * 2, 0, iz * 2);
    }

    /** Разговор с курьером (как знакомство с Тимофеем в начале). */
    public static void talk(ServerPlayer p, CityNpcEntity c) {
        if (!p.getUUID().equals(c.owner)) { Story.say(p, c.role.name, c.role.color, "Это не ваш заказ. Не трогайте рюкзак, пожалуйста."); return; }
        StoryData d = Progress.d(p);
        if (d.food.getInt("state") != 2) return;
        boolean late = d.food.getBoolean("late");
        int num = apt(p) == null ? 0 : apt(p).number;
        int fee = d.food.getInt("fee");
        String R = c.role.name; int C = c.role.color;
        DialogueManager.Dialogue dl = new DialogueManager.Dialogue("a")
                .node("a", R, C, late ? "«Шпиль.Еда»! Квартира " + num + "? Простите-простите, опоздал: лифт стоял, на Тракте пробка из лодок, а у Пустоши кто-то кричал «Громила!»…"
                                : "«Шпиль.Еда»! Квартира " + num + ", {name}? Всё как заказывали — ещё тёплое. Почти.",
                        opt("Да, это я", "b"))
                .node("b", R, C, late ? "Держите заказ. За опоздание доставка за наш счёт — вернул " + fee + " ✦. Только не пишите в отзывах про крыс."
                                : "Держите! Рюкзак не трогайте — он тяжелее, чем выглядит.",
                        pp -> deliver(pp, late), opt("Спасибо", "c"))
                .node("c", R, C, "Оцените доставку? Мне за каждую звезду дают премию. Ну, обещают.", opt("★★★★★", "d5"), opt("★☆☆☆☆", "d1"))
                .node("d5", R, C, "Пять звёзд! Это мне на новый рюкзак. Приятного аппетита, {name}!", opt("[Закрыть]", null))
                .node("d1", R, C, "Одна звезда… А я так бежал. Ладно. Приятного аппетита.", opt("[Закрыть]", null))
                .onEnd(pp -> leave(c)).onAbort(pp -> leave(c));
        DialogueManager.open(p, c, dl);
    }

    private static void deliver(ServerPlayer p, boolean late) {
        StoryData d = Progress.d(p);
        if (d.food.getInt("state") != 2) return;
        for (String part : d.food.getString("cart").split(",")) {
            String[] kv = part.split(":");
            if (kv.length != 2) continue;
            int i = Integer.parseInt(kv[0]), q = Integer.parseInt(kv[1]);
            if (i < 0 || i >= MENU.length) continue;
            Bag.give(p, new ItemStack(MENU[i].item.get(), q * MENU[i].count));
        }
        int fee = d.food.getInt("fee");
        d.food = new CompoundTag();
        p.setData(ModRegistry.STORY.get(), d);
        if (late) Progress.addMoney(p, fee, false);
        Story.sync(p);
    }

    private static void leave(CityNpcEntity c) {
        if (!c.isAlive()) return;
        c.owner = null;
        c.leaveAt = c.tickCount + 40;
    }

    private static void msg(ServerPlayer p, String s, ChatFormatting col) { p.sendSystemMessage(Component.literal(s).withStyle(col)); }
}
