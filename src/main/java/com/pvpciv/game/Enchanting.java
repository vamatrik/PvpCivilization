package com.pvpciv.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pvpciv.data.StoryData;
import com.pvpciv.inv.Bag;
import com.pvpciv.item.CivArmorItem;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModF4;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/**
 * 0.11 — молекулярное зачарование (этаж 4, стол Чарователя в вестибюле NaZa).
 *  • Ресурс — лазуратрон (без качества). Цена — 6 × тир предмета за попытку; режимы x1/x3/x10 тратят столько же раз больше.
 *  • Модули (ПКМ рядом со столом) встают в следующее свободное гнездо (8 гнёзд, у каждого игрока свои): каждый модуль — +1 вариант,
 *    лучший модуль задаёт потолок ранга (Базовый — II, Улучшенный — III, Продвинутый — IV).
 *  • Режим: x3 — +1 вариант и ранг «лучший из 2», x10 — +2 варианта и «лучший из 3». Режим xN забирает до N катализаторов — все действуют:
 *    «Фокус» +1 вариант, «Якорь» — можно оставить старое, «Резонатор» — +1 ранг случайному варианту.
 *  • Игрок выбирает 1 из N; новое зачарование заменяет старое.
 * Список доступных чар растёт по квестам Чарователя.
 */
public final class Enchanting {
    private Enchanting() {}

    public record Ench(String id, String name, boolean weapon, String fmt, double[] v, int quest) {
        public String desc(int rank) { return String.format(Locale.ROOT, fmt, v[Math.min(rank, 4) - 1]); }
    }

    public static final List<Ench> ALL = List.of(
            new Ench("sharp", "Нанозаточка", true, "Урон +%.0f%%", new double[]{5, 9, 14, 20}, 1),
            new Ench("frost", "Криоудар", true, "%.0f%% шанс замедлить цель", new double[]{10, 16, 24, 35}, 1),
            new Ench("vamp", "Вампиризм", true, "Лечит %.0f%% нанесённого урона", new double[]{2, 4, 6, 9}, 2),
            new Ench("volt", "Разряд", true, "Каждый 4-й удар: +%.0f%% урона", new double[]{40, 70, 100, 150}, 3),
            new Ench("exec", "Казнь", true, "+%.0f%% урона по целям ниже 30%% HP", new double[]{15, 25, 40, 60}, 3),
            new Ench("pierce", "Пробой", true, "Игнорирует %.0f%% брони цели", new double[]{10, 18, 26, 35}, 4),
            new Ench("guard", "Бронеплёнка", false, "Входящий урон −%.0f%%", new double[]{2, 3, 4.5, 6}, 1),
            new Ench("servo", "Сервопривод", false, "Скорость +%.0f%%", new double[]{2, 3, 4.5, 6}, 1),
            new Ench("regen", "Регенератор", false, "Лечит %.2f%% HP в секунду", new double[]{0.1, 0.18, 0.27, 0.4}, 2),
            new Ench("thorns", "Отражатель", false, "Возвращает %.0f%% урона ближнего боя", new double[]{5, 8, 12, 16}, 3),
            new Ench("dodge", "Фазовый сдвиг", false, "%.1f%% шанс увернуться", new double[]{1.5, 2.5, 3.5, 5}, 4));

    public static Ench byId(String id) { for (Ench e : ALL) if (e.id.equals(id)) return e; return null; }
    private static final String[] ROMAN = {"", "I", "II", "III", "IV"};

    // ------------------------------------------------------------------ данные на предмете
    public static String get(ItemStack st) { String s = st.get(ModRegistry.ENCH.get()); return s == null ? "" : s; }
    public static Ench ench(ItemStack st) { String s = get(st); if (s.isEmpty()) return null; return byId(s.split(":")[0]); }
    public static int rank(ItemStack st) { String s = get(st); if (s.isEmpty()) return 0; try { return Integer.parseInt(s.split(":")[1]); } catch (Exception e) { return 1; } }

    public static void tip(ItemStack st, List<Component> tip) {
        Ench e = ench(st);
        if (e == null) return;
        int r = rank(st);
        tip.add(Component.literal("⚛ " + e.name + " " + ROMAN[Math.min(4, r)] + ": " + e.desc(r)).withStyle(x -> x.withColor(0x40D0FF)));
    }

    /** Сумма значения чары по предметам (оружие — основная рука, броня — 4 слота). */
    public static double val(LivingEntity p, String id) {
        double s = 0;
        Ench e = byId(id);
        if (e == null) return 0;
        if (e.weapon) { ItemStack w = p.getMainHandItem(); if (id.equals(eid(w))) s += e.v[rank(w) - 1]; }
        else for (EquipmentSlot sl : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack a = p.getItemBySlot(sl);
            if (id.equals(eid(a))) s += e.v[rank(a) - 1];
        }
        return s;
    }
    private static String eid(ItemStack st) { String s = get(st); return s.isEmpty() ? "" : s.split(":")[0]; }

    // ------------------------------------------------------------------ боевые эффекты
    private static final Map<UUID, Integer> HITS = new HashMap<>();

    /** Игрок наносит удар (до брони): множители урона и замедление. */
    public static float onAttack(ServerPlayer p, LivingEntity t, float amount, boolean melee) {
        if (!melee) return amount;
        double k = 1 + val(p, "sharp") / 100;
        if (t.getHealth() < t.getMaxHealth() * 0.3) k += val(p, "exec") / 100;
        double volt = val(p, "volt");
        if (volt > 0) {
            int n = HITS.merge(p.getUUID(), 1, Integer::sum);
            if (n % 4 == 0) {
                k += volt / 100;
                if (p.level() instanceof ServerLevel sl) sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 14, 0.3, 0.4, 0.3, 0.2);
            }
        }
        double fr = val(p, "frost");
        if (fr > 0 && p.getRandom().nextDouble() * 100 < fr)
            t.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
        return (float) (amount * k);
    }

    /** Доля брони цели, которую игнорирует удар игрока. */
    public static double pen(ServerPlayer p) { return val(p, "pierce") / 100; }

    /** После урона: вампиризм. */
    public static void onDealt(ServerPlayer p, float dmg) {
        double v = val(p, "vamp");
        if (v > 0 && dmg > 0) p.heal((float) (dmg * v / 100));
    }

    /** По игроку бьют: уворот (true — отменить удар), снижение урона, отражение. */
    public static boolean dodge(ServerPlayer p) {
        double d = val(p, "dodge");
        if (d > 0 && p.getRandom().nextDouble() * 100 < d) {
            if (p.level() instanceof ServerLevel sl) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 20, 0.3, 0.6, 0.3, 0.3);
                com.pvpciv.combat.DamageNumbers.show(sl, p, Component.literal("УВОРОТ").withStyle(x -> x.withColor(0x40D0FF).withBold(true)));
            }
            return true;
        }
        return false;
    }
    public static float onDefend(ServerPlayer p, LivingEntity src, float amount, boolean melee) {
        float out = (float) (amount * (1 - Math.min(0.5, val(p, "guard") / 100)));
        double th = val(p, "thorns");
        if (th > 0 && melee && src != null && src != p) {
            float back = (float) (amount * th / 100);
            src.hurt(p.damageSources().thorns(p), back);
        }
        return out;
    }

    private static final net.minecraft.resources.ResourceLocation SERVO = com.pvpciv.PvpCiv.id("ench_servo");
    /** Раз в секунду: регенерация и скорость сервопривода. */
    public static void tick(ServerPlayer p) {
        if (p.tickCount % 20 != 0) return;
        double rg = val(p, "regen");
        if (rg > 0 && p.getHealth() < p.getMaxHealth() && p.getLastHurtByMobTimestamp() + 100 < p.tickCount) p.heal((float) (p.getMaxHealth() * rg / 100));
        var attr = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        double sv = val(p, "servo") / 100;
        var cur = attr.getModifier(SERVO);
        if (sv <= 0) { if (cur != null) attr.removeModifier(SERVO); return; }
        if (cur != null && Math.abs(cur.amount() - sv) < 1e-6) return;
        attr.removeModifier(SERVO);
        attr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(SERVO, sv, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    // ------------------------------------------------------------------ стол: модули
    public static final int SOCKETS = 8;

    public static int[] modules(StoryData d) {
        int[] m = d.gates.getIntArray("ench_mods");
        return m.length == SOCKETS ? m : new int[SOCKETS];
    }

    /** ПКМ модулем рядом со столом — модуль встаёт в следующее гнездо (или заменяет худший, если все заняты). */
    public static boolean placeModule(ServerPlayer p, ItemStack st, int tier) {
        if (!com.pvpciv.world.CivWorld.isF4(p.level()) || p.blockPosition().distSqr(com.pvpciv.floor.Floor4.ENCH_TABLE) > 8 * 8) {
            p.displayClientMessage(Component.literal("Модуль ставится рядом с молекулярным столом (НТЦ NaZa, вестибюль)").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        StoryData d = Progress.d(p);
        int[] m = modules(d);
        int slot = -1;
        for (int i = 0; i < SOCKETS; i++) if (m[i] == 0) { slot = i; break; }
        if (slot < 0) {
            int worst = 0;
            for (int i = 1; i < SOCKETS; i++) if (m[i] < m[worst]) worst = i;
            if (m[worst] >= tier) { p.displayClientMessage(Component.literal("Все 8 гнёзд заняты модулями не хуже этого").withStyle(ChatFormatting.GRAY), true); return false; }
            slot = worst;
        }
        m[slot] = tier;
        d.gates.putIntArray("ench_mods", m);
        p.setData(ModRegistry.STORY.get(), d);
        st.shrink(1);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.8f, 1.4f);
        p.displayClientMessage(Component.literal("⚛ Модуль встал в гнездо " + (slot + 1) + "/" + SOCKETS).withStyle(x -> x.withColor(0x40D0FF)), true);
        return true;
    }

    // ------------------------------------------------------------------ стол: синтез
    private record Opt(String id, int rank) {}
    private record Pending(List<Opt> opts, boolean anchor, int slot) {}
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    public static int cost(ItemStack st) {
        int t = st.getItem() instanceof CivWeaponItem w ? w.tier : st.getItem() instanceof CivArmorItem a ? (int) Math.floor(a.tier) : 0;
        return 6 * Math.max(1, t);
    }

    private static boolean ok(ItemStack st) { return st.getItem() instanceof CivWeaponItem || st.getItem() instanceof CivArmorItem; }
    private static boolean weapon(ItemStack st) { return st.getItem() instanceof CivWeaponItem; }

    public static int questStage(StoryData d) { return d.gates.getInt("ench_q"); }

    /** ПКМ по столу: открыть экран. */
    public static void open(ServerPlayer p) {
        StoryData d = Progress.d(p);
        if (questStage(d) < 1) {
            Story.say(p, "Молекулярный стол", 0x40D0FF, "ДОСТУП ЗАПРЕЩЁН. Предъявите пропуск сотрудника NaZa. (Поговори с Чарователем Аурелием.)");
            return;
        }
        sync(p);
    }

    public static void sync(ServerPlayer p) {
        StoryData d = Progress.d(p);
        ItemStack st = p.getMainHandItem();
        JsonObject o = new JsonObject();
        o.addProperty("ok", ok(st));
        o.addProperty("item", st.isEmpty() ? "" : st.getHoverName().getString());
        o.addProperty("weapon", weapon(st));
        o.addProperty("cur", ench(st) == null ? "" : ench(st).name + " " + ROMAN[Math.min(4, rank(st))] + " — " + ench(st).desc(rank(st)));
        o.addProperty("cost", ok(st) ? cost(st) : 0);
        o.addProperty("laz", Bag.count(p, s -> s.is(ModF4.LAZURATRON.get())));
        o.addProperty("focus", Bag.count(p, s -> s.is(ModF4.CAT_FOCUS.get())));
        o.addProperty("anchor", Bag.count(p, s -> s.is(ModF4.CAT_ANCHOR.get())));
        o.addProperty("reso", Bag.count(p, s -> s.is(ModF4.CAT_RESONATOR.get())));
        JsonArray mods = new JsonArray();
        for (int m : modules(d)) mods.add(m);
        o.add("mods", mods);
        JsonArray pool = new JsonArray();
        for (Ench e : ALL) if (e.weapon == weapon(st) && e.quest <= questStage(d)) pool.add(e.name);
        o.add("pool", pool);
        Pending pd = PENDING.get(p.getUUID());
        JsonArray opts = new JsonArray();
        if (pd != null && pd.slot == p.getInventory().selected) for (Opt op : pd.opts) {
            Ench e = byId(op.id);
            JsonObject j = new JsonObject();
            j.addProperty("name", e.name + " " + ROMAN[op.rank]);
            j.addProperty("desc", e.desc(op.rank));
            j.addProperty("rank", op.rank);
            opts.add(j);
        }
        o.add("opts", opts);
        o.addProperty("keep", pd != null && pd.anchor);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(19, o.toString()));
    }

    public static void cmd(ServerPlayer p, String c) {
        String[] a = c.split(":");
        if (a.length < 2) return;
        if (!com.pvpciv.world.CivWorld.isF4(p.level()) || p.blockPosition().distSqr(com.pvpciv.floor.Floor4.ENCH_TABLE) > 10 * 10) return;
        switch (a[1]) {
            case "open" -> sync(p);
            case "roll" -> { try { roll(p, Integer.parseInt(a[2])); } catch (Exception ignored) {} }
            case "pick" -> { try { pick(p, Integer.parseInt(a[2])); } catch (Exception ignored) {} }
        }
    }

    private static void roll(ServerPlayer p, int mode) {
        if (mode != 1 && mode != 3 && mode != 10) return;
        ItemStack st = p.getMainHandItem();
        if (!ok(st)) { p.displayClientMessage(Component.literal("Возьми в руку оружие или броню").withStyle(ChatFormatting.RED), true); return; }
        StoryData d = Progress.d(p);
        int price = cost(st) * mode;
        if (Bag.count(p, s -> s.is(ModF4.LAZURATRON.get())) < price) { p.displayClientMessage(Component.literal("Нужно лазуратрона: " + price).withStyle(ChatFormatting.RED), true); return; }
        Bag.take(p, s -> s.is(ModF4.LAZURATRON.get()), price);
        int nMods = 0, best = 0;
        for (int m : modules(d)) if (m > 0) { nMods++; best = Math.max(best, m); }
        int cap = Math.max(2, best + 1);                       // базовый — до II, улучшенный — III, продвинутый — IV
        int left = mode;                                       // режим xN забирает до N катализаторов
        int focus = Math.min(left, Bag.count(p, s -> s.is(ModF4.CAT_FOCUS.get())));
        left -= focus;
        int anchor = Math.min(Math.min(left, 1), Bag.count(p, s -> s.is(ModF4.CAT_ANCHOR.get())));
        left -= anchor;
        int reso = Math.min(left, Bag.count(p, s -> s.is(ModF4.CAT_RESONATOR.get())));
        if (focus > 0) Bag.take(p, s -> s.is(ModF4.CAT_FOCUS.get()), focus);
        if (anchor > 0) Bag.take(p, s -> s.is(ModF4.CAT_ANCHOR.get()), anchor);
        if (reso > 0) Bag.take(p, s -> s.is(ModF4.CAT_RESONATOR.get()), reso);
        int n = Math.min(8, Math.max(1, nMods) + (mode == 3 ? 1 : mode == 10 ? 2 : 0) + focus);
        int tries = mode == 1 ? 1 : mode == 3 ? 2 : 3;
        List<Ench> pool = new ArrayList<>();
        for (Ench e : ALL) if (e.weapon == weapon(st) && e.quest <= questStage(d)) pool.add(e);
        Collections.shuffle(pool, new Random(p.getRandom().nextLong()));
        List<Opt> opts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Ench e = pool.get(i % pool.size());
            int r = 1;
            for (int t = 0; t < tries; t++) r = Math.max(r, rollRank(p, cap));
            opts.add(new Opt(e.id, r));
        }
        for (int i = 0; i < reso; i++) {
            int k = p.getRandom().nextInt(opts.size());
            opts.set(k, new Opt(opts.get(k).id, Math.min(4, opts.get(k).rank + 1)));
        }
        PENDING.put(p.getUUID(), new Pending(opts, anchor > 0 || ench(st) == null, p.getInventory().selected));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 0.8f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 1.6f);
        p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, com.pvpciv.floor.Floor4.ENCH_TABLE.getX() + 0.5, com.pvpciv.floor.Floor4.ENCH_TABLE.getY() + 1.5,
                com.pvpciv.floor.Floor4.ENCH_TABLE.getZ() + 0.5, 60, 0.8, 0.6, 0.8, 0.6);
        sync(p);
    }

    /** Ранг 1…cap: I — 60%, II — 28%, III — 10%, IV — 2% (выше потолка — срезается). */
    private static int rollRank(ServerPlayer p, int cap) {
        double x = p.getRandom().nextDouble();
        int r = x < 0.60 ? 1 : x < 0.88 ? 2 : x < 0.98 ? 3 : 4;
        return Math.min(r, cap);
    }

    private static void pick(ServerPlayer p, int i) {
        Pending pd = PENDING.get(p.getUUID());
        if (pd == null) return;
        ItemStack st = p.getMainHandItem();
        if (pd.slot != p.getInventory().selected || !ok(st)) { p.displayClientMessage(Component.literal("Предмет сменился — синтез сброшен").withStyle(ChatFormatting.RED), true); PENDING.remove(p.getUUID()); sync(p); return; }
        PENDING.remove(p.getUUID());
        if (i < 0) {
            if (!pd.anchor) return;
            p.displayClientMessage(Component.literal("Старое зачарование сохранено").withStyle(ChatFormatting.GRAY), true);
            sync(p);
            return;
        }
        if (i >= pd.opts.size()) return;
        Opt o = pd.opts.get(i);
        st.set(ModRegistry.ENCH.get(), o.id + ":" + o.rank);
        Ench e = byId(o.id);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.6f);
        p.displayClientMessage(Component.literal("⚛ " + e.name + " " + ROMAN[o.rank] + " — " + e.desc(o.rank)).withStyle(x -> x.withColor(0x40D0FF)), false);
        if (o.rank >= 4) com.pvpciv.progress.Titles.grant(p, "molecular");
        sync(p);
    }
}
