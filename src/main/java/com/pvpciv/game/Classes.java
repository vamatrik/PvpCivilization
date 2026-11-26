package com.pvpciv.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pvpciv.data.StoryData;
import com.pvpciv.item.CivWeaponItem;
import com.pvpciv.item.CivWeaponItem.WClass;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.registry.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Predicate;

/**
 * 1.2 — Древо классов. В центре — «Карманный телепорт» (слот 3), от него шесть ветвей (каждые 60°):
 * Воин, Чародей клинка*, Маг*, Стрелок*, Ассасин, Страж (* — «скоро»).
 * Ветвь: класс с пассивкой → две цепочки по 2 стата (можно взять обе) → навык класса (слот 4).
 * Цены: центр 2, класс 1, стат 1, навык 2. Очки древа: первое прохождение испытания Луны — 4, следующие — по 3.
 * Бонусы урона, скорости атаки и критов — только с оружием своего класса; здоровье, защита, реген, скорость, уклонение — всегда.
 */
public final class Classes {
    private Classes() {}

    public enum PClass {
        WARRIOR("Воин", 0x6AC8FF, "Меч (1.6 удара/с). Урон и вампиризм — универсал.", true, WClass.SWORD),
        SPELLBLADE("Чародей клинка", 0xFF70C0, "Меч, наполненный магией. Скоро.", false),
        MAGE("Маг", 0x6090FF, "Магия. Скоро.", false),
        ARCHER("Стрелок", 0x80D060, "Огнестрел. Скоро.", false),
        ASSASSIN("Ассасин", 0xB070FF, "Парные клинки. Скорость, криты, уклонение.", true, WClass.DAGGER, WClass.SABER),
        GUARD("Страж", 0xFFB040, "Топоры и молоты. Здоровье, защита, регенерация.", true, WClass.AXE, WClass.HAMMER);
        public final String title, desc; public final int color; public final boolean open; public final Set<WClass> weapons;
        PClass(String t, int c, String d, boolean open, WClass... w) {
            title = t; color = c; desc = d; this.open = open;
            weapons = w.length == 0 ? EnumSet.noneOf(WClass.class) : EnumSet.copyOf(Arrays.asList(w));
        }
        public static PClass of(String s) { for (PClass c : values()) if (c.name().equals(s)) return c; return null; }
    }

    /** Статы узлов (в процентах). */
    public enum Stat {
        HP("здоровья", false), DEF("защиты", false), REGEN("HP/с реген", false), DMG("урона", true), LIFESTEAL("вампиризма", true),
        ASPD("скорости атаки", true), MSPD("скорости бега", false), CRIT("шанса крита", true), CRITDMG("крит-урона", true), DODGE("уклонения", false);
        public final String title; public final boolean weapon;
        Stat(String t, boolean w) { title = t; weapon = w; }
    }

    public enum Kind { CENTER, BASE, STAT, SKILL }

    /** req — нужен хотя бы один из узлов. */
    public record Node(String id, PClass cls, Kind kind, String name, String desc, int cost, List<String> req, Map<Stat, Double> stats, String skill) {}

    private static Map<Stat, Double> st(Object... kv) {
        Map<Stat, Double> m = new EnumMap<>(Stat.class);
        for (int i = 0; i < kv.length; i += 2) m.put((Stat) kv[i], ((Number) kv[i + 1]).doubleValue());
        return m;
    }

    private static final List<Node> NODES_L = new ArrayList<>();
    public static final List<Node> NODES = Collections.unmodifiableList(NODES_L);

    private static void branch(PClass c, String p, String baseName, String baseDesc, Map<Stat, Double> base,
                               String a1n, Map<Stat, Double> a1, String a2n, Map<Stat, Double> a2,
                               String b1n, Map<Stat, Double> b1, String b2n, Map<Stat, Double> b2,
                               String skill, String skillName, String skillDesc) {
        NODES_L.add(new Node(p + "_base", c, Kind.BASE, baseName, baseDesc, 1, List.of("tp"), base, null));
        NODES_L.add(new Node(p + "_a1", c, Kind.STAT, a1n, "", 1, List.of(p + "_base"), a1, null));
        NODES_L.add(new Node(p + "_a2", c, Kind.STAT, a2n, "", 1, List.of(p + "_a1"), a2, null));
        NODES_L.add(new Node(p + "_b1", c, Kind.STAT, b1n, "", 1, List.of(p + "_base"), b1, null));
        NODES_L.add(new Node(p + "_b2", c, Kind.STAT, b2n, "", 1, List.of(p + "_b1"), b2, null));
        NODES_L.add(new Node(p + "_skill", c, Kind.SKILL, skillName, skillDesc, 2, List.of(p + "_a2", p + "_b2"), Map.of(), skill));
    }

    static {
        NODES_L.add(new Node("tp", null, Kind.CENTER, "Карманный телепорт",
                "Навык слота 3: 3 с поднимаешь руки — и открывается телепорт, как у врат этажа, где стоишь (по этажу, вниз и домой — всегда; вверх — по улучшениям врат). Урон прерывает каст. Перезарядка 180 с.",
                2, List.of(), Map.of(), "pocket"));
        branch(PClass.WARRIOR, "w", "Мастерство клинка", "Класс Воин. Пассивно: +5% урона мечом, вампиризм 1.5%.", st(Stat.DMG, 5, Stat.LIFESTEAL, 1.5),
                "Острота", st(Stat.DMG, 5), "Темп", st(Stat.ASPD, 4),
                "Закалка", st(Stat.HP, 5), "Марш", st(Stat.MSPD, 3),
                "rage", "Ярость", "Слот 4: 8 с — +30% урона, −20% входящего урона, +10% скорости атаки, иммунитет к оглушению и отбросу. Убийство продлевает на 1 с (до 14 с). Перезарядка 45 с.");
        branch(PClass.ASSASSIN, "a", "Тень", "Класс Ассасин. Пассивно: +5% скорости бега, +4% шанса крита.", st(Stat.MSPD, 5, Stat.CRIT, 4),
                "Вихрь", st(Stat.ASPD, 5), "Ускользание", st(Stat.DODGE, 3),
                "Точность", st(Stat.CRIT, 4), "Смертельный удар", st(Stat.CRITDMG, 15),
                "retreat", "Отступление", "Слот 4: 3 с полной невидимости (не видно даже брони), мобы сбрасывают агр. Первый удар из тени — гарантированный крит и снимает невидимость. Перезарядка 35 с.");
        branch(PClass.GUARD, "g", "Каменная кожа", "Класс Страж. Пассивно: +6% здоровья, +5% защиты, реген 0.2% HP/с.", st(Stat.HP, 6, Stat.DEF, 5, Stat.REGEN, 0.2),
                "Крепость", st(Stat.HP, 5), "Броня духа", st(Stat.DEF, 6),
                "Натиск", st(Stat.DMG, 5), "Второе дыхание", st(Stat.REGEN, 0.3),
                "bastion", "Бастион", "Слот 4: 7 с поглощаешь 80% урона (медленнее на 15%). Следующий удар в течение 5 с добавляет поглощённое (до ×4 урона оружия). Перезарядка 40 с.");
    }

    public static final int MAX_FORGET_HOURS = 24;

    public static Node node(String id) { for (Node n : NODES) if (n.id.equals(id)) return n; return null; }
    public static PClass cls(StoryData d) { return PClass.of(d.gates.getString("cls")); }
    public static boolean has(StoryData d, String node) { return d.gates.getString("cls_n").contains("," + node + ","); }
    public static boolean has(ServerPlayer p, String node) { return has(Progress.d(p), node); }

    public static boolean classWeapon(PClass c, ItemStack st) { return c != null && st.getItem() instanceof CivWeaponItem w && c.weapons.contains(w.cls); }
    /** Оружие класса — для перекоса лута. */
    public static Predicate<Item> prefer(ServerPlayer p) {
        PClass c = p == null ? null : cls(Progress.d(p));
        if (c == null || c.weapons.isEmpty()) return null;
        return it -> it instanceof CivWeaponItem w && c.weapons.contains(w.cls);
    }

    /** Навык слота 4 (по классу) или "". */
    public static String skill4(StoryData d) {
        for (Node n : NODES) if (n.kind == Kind.SKILL && has(d, n.id)) return n.skill;
        return "";
    }

    /** Сумма стата по изученным узлам (с учётом «только с оружием класса»). */
    public static double stat(ServerPlayer p, Stat s) {
        StoryData d = Progress.d(p);
        PClass c = cls(d);
        if (s.weapon && !classWeapon(c, com.pvpciv.combat.Melee.striking(p))) return 0;
        double v = 0;
        for (Node n : NODES) { Double x = n.stats.get(s); if (x != null && has(d, n.id)) v += x; }
        return v;
    }

    // ------------------------------------------------------------------ очки древа
    /** Очки древа (ключ "dm" — исторически «тёмная материя»). */
    public static int matter(ServerPlayer p) { return Progress.d(p).gates.getInt("dm"); }
    public static void addMatter(StoryData d, int n) { d.gates.putInt("dm", Math.max(0, d.gates.getInt("dm") + n)); }
    public static void giveMatter(ServerPlayer p, int n) {
        StoryData d = Progress.d(p);
        addMatter(d, n);
        p.setData(ModRegistry.STORY.get(), d);
        p.displayClientMessage(Component.literal("+" + n + " очк. древа классов (всего " + matter(p) + ")").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        sync(p, false);
    }
    /** Сколько очков даёт первое прохождение стадии испытания Луны. */
    public static int lunaPoints(int stage) { return stage <= 1 ? 4 : 3; }

    /** 1.2: переход на новое древо — старый класс сброшен, очки пересчитаны по пройденным испытаниям. */
    public static void migrate(ServerPlayer p) {
        StoryData d = Progress.d(p);
        if (d.gates.getBoolean("tree2")) return;
        int best = d.gates.getInt("luna_best");
        int pts = 0;
        for (int s = 1; s <= best; s++) pts += lunaPoints(s);
        if (d.gates.getInt("demon_pts") > 0 || (d.trophies & 512) != 0) pts += 2;   // демоница
        boolean had = !d.gates.getString("cls").isEmpty();
        d.gates.remove("cls"); d.gates.remove("cls_n");
        d.gates.putInt("dm", Math.max(pts, 0));
        d.gates.putInt("dm_spent", 0);
        d.gates.putBoolean("tree2", true);
        d.skill3 = "";
        p.setData(ModRegistry.STORY.get(), d);
        if (had || pts > 0) p.sendSystemMessage(Component.literal("✦ 1.2: классы переделаны в единое древо. Старый класс сброшен, очки древа пересчитаны: " + pts + ". Древо — Герой → Навыки → «Класс».").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static boolean pay(ServerPlayer p, int n) {
        if (matter(p) < n) { p.displayClientMessage(Component.literal("Нужно очков древа: " + n + " (есть " + matter(p) + "). Их дают первые прохождения испытаний Луны.").withStyle(ChatFormatting.RED), true); return false; }
        StoryData d = Progress.d(p);
        addMatter(d, -n);
        d.gates.putInt("dm_spent", d.gates.getInt("dm_spent") + n);
        p.setData(ModRegistry.STORY.get(), d);
        return true;
    }

    public static void cmd(ServerPlayer p, String cmd) {
        String[] a = cmd.split(":");
        if (a.length < 2) return;
        switch (a[1]) {
            case "open" -> sync(p, true);
            case "node", "pick" -> { if (a.length == 3) learn(p, a[2]); }
            default -> {}
        }
    }

    /** Можно ли взять узел (без учёта очков). null — можно, иначе причина. */
    public static String why(StoryData d, Node n) {
        if (has(d, n.id)) return "уже изучено";
        PClass c = cls(d);
        if (n.cls != null) {
            if (!n.cls.open) return "скоро";
            if (c != null && c != n.cls) return "у тебя другой класс";
        }
        if (!n.req.isEmpty()) {
            boolean ok = false;
            for (String r : n.req) if (has(d, r)) ok = true;
            if (!ok) return n.kind == Kind.BASE ? "сначала — центр древа" : n.kind == Kind.SKILL ? "пройди одну из цепочек" : "сначала — предыдущий узел";
        }
        return null;
    }

    public static void learn(ServerPlayer p, String id) {
        StoryData d = Progress.d(p);
        Node n = node(id);
        if (n == null) return;
        String w = why(d, n);
        if (w != null) { p.displayClientMessage(Component.literal("Нельзя: " + w + ".").withStyle(ChatFormatting.GRAY), true); return; }
        if (!pay(p, n.cost)) { sync(p, false); return; }
        d = Progress.d(p);
        d.gates.putString("cls_n", (d.gates.getString("cls_n").isEmpty() ? "," : d.gates.getString("cls_n")) + id + ",");
        if (n.kind == Kind.BASE) d.gates.putString("cls", n.cls.name());
        p.setData(ModRegistry.STORY.get(), d);
        int col = n.cls == null ? 0xC090FF : n.cls.color;
        Story.title(p, n.name, n.kind == Kind.BASE ? "Класс выбран!" : n.kind == Kind.STAT ? "Узел изучен" : "Новый навык!", col);
        p.serverLevel().playSound(null, p.blockPosition(), n.kind == Kind.BASE ? SoundEvents.TOTEM_USE : SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.4f);
        if (n.kind == Kind.BASE) com.pvpciv.progress.Titles.grant(p, "cls_" + n.cls.name().toLowerCase(Locale.ROOT));
        if (n.skill != null) p.sendSystemMessage(Component.literal("✦ Навык слота " + (n.kind == Kind.CENTER ? "3" : "4") + ": " + n.name).withStyle(ChatFormatting.LIGHT_PURPLE));
        applyStats(p);
        sync(p, false);
        Skills.sync(p);
    }

    /** Свиток забвения: класс и его узлы сброшены, очки возвращаются (центр древа остаётся). */
    public static boolean forget(ServerPlayer p) {
        StoryData d = Progress.d(p);
        if (cls(d) == null) { p.displayClientMessage(Component.literal("Нечего забывать: класс не выбран.").withStyle(ChatFormatting.GRAY), true); return false; }
        long now = System.currentTimeMillis(), next = d.gates.getLong("forget_at");
        if (now < next) {
            long h = (next - now) / 3_600_000, m = (next - now) / 60_000 % 60;
            p.displayClientMessage(Component.literal("Память ещё не остыла: свиток сработает через " + h + " ч " + m + " мин.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        int back = 0;
        StringBuilder keep = new StringBuilder(",");
        for (Node n : NODES) if (has(d, n.id)) { if (n.cls == null) keep.append(n.id).append(','); else back += n.cost; }
        d.gates.remove("cls");
        d.gates.putString("cls_n", keep.toString());
        d.gates.putInt("dm_spent", Math.max(0, d.gates.getInt("dm_spent") - back));
        d.gates.putLong("forget_at", now + MAX_FORGET_HOURS * 3_600_000L);
        addMatter(d, back);
        p.setData(ModRegistry.STORY.get(), d);
        Story.title(p, "Забвение", "Класс сброшен. Очки древа возвращены: " + back, 0xC0A0FF);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 0.5f);
        applyStats(p);
        sync(p, false);
        Skills.sync(p);
        return true;
    }

    public static void sync(ServerPlayer p, boolean open) {
        migrate(p);
        StoryData d = Progress.d(p);
        JsonObject j = new JsonObject();
        PClass c = cls(d);
        j.addProperty("open", open);
        j.addProperty("cls", c == null ? "" : c.name());
        j.addProperty("dm", matter(p));
        j.addProperty("spent", d.gates.getInt("dm_spent"));
        j.addProperty("comboMax", comboMax(p));
        JsonArray cl = new JsonArray();
        for (PClass k : PClass.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", k.name()); o.addProperty("name", k.title); o.addProperty("desc", k.desc); o.addProperty("color", k.color); o.addProperty("open", k.open);
            StringBuilder w = new StringBuilder();
            for (WClass wc : k.weapons) w.append(w.length() > 0 ? ", " : "").append(wc.title);
            o.addProperty("weapons", w.toString());
            cl.add(o);
        }
        j.add("classes", cl);
        JsonArray ns = new JsonArray();
        for (Node n : NODES) {
            JsonObject o = new JsonObject();
            o.addProperty("id", n.id); o.addProperty("cls", n.cls == null ? "" : n.cls.name()); o.addProperty("kind", n.kind.name());
            o.addProperty("name", n.name); o.addProperty("desc", n.desc.isEmpty() ? statText(n) : n.desc); o.addProperty("cost", n.cost);
            o.addProperty("have", has(d, n.id));
            String w = why(d, n);
            o.addProperty("why", w == null ? "" : w);
            ns.add(o);
        }
        j.add("nodes", ns);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(23, j.toString()));
    }

    private static String statText(Node n) {
        StringBuilder b = new StringBuilder();
        for (var e : n.stats.entrySet()) {
            double v = e.getValue();
            b.append(b.length() > 0 ? ", " : "").append("+").append(v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v)).append("% ").append(e.getKey().title);
        }
        if (n.stats.keySet().stream().anyMatch(s -> s.weapon)) b.append(" (с оружием класса)");
        return b.append(".").toString();
    }

    // ------------------------------------------------------------------ бой
    public static int comboMax(ServerPlayer p) { return com.pvpciv.combat.Melee.COMBO_MAX; }

    private static final Map<UUID, Long> RAGE = new HashMap<>(), RAGE_START = new HashMap<>(), RETREAT = new HashMap<>();
    private static final Map<UUID, float[]> BASTION = new HashMap<>();   // {до (тик), накоплено}
    private static final Map<UUID, float[]> BASTION_HIT = new HashMap<>();   // {до (тик), урон}
    private static final Set<UUID> NEXT_CRIT = new HashSet<>();
    public static final Set<UUID> CD_RESET = new HashSet<>();

    private static long now(ServerPlayer p) { return p.serverLevel().getGameTime(); }

    public static boolean raging(ServerPlayer p) { return RAGE.getOrDefault(p.getUUID(), 0L) > now(p); }
    public static boolean hidden(LivingEntity e) { return e instanceof ServerPlayer p && RETREAT.getOrDefault(p.getUUID(), 0L) > now(p); }

    /** Гарантированный крит этого удара (удар из «Отступления»). */
    public static boolean forceCrit(ServerPlayer p, LivingEntity t) { return NEXT_CRIT.remove(p.getUUID()); }

    public static double critBonus(ServerPlayer p) { return stat(p, Stat.CRIT); }
    public static double critDmgBonus(ServerPlayer p) { return stat(p, Stat.CRITDMG); }
    public static double dodge(ServerPlayer p) { return stat(p, Stat.DODGE); }

    /** Удар игрока (после уровней и критов). */
    public static float onAttack(ServerPlayer p, LivingEntity t, float amount, boolean melee) {
        if (!melee) return amount;
        StoryData d = Progress.d(p);
        if (cls(d) == null) return amount;
        long now = now(p);
        amount *= 1 + (float) stat(p, Stat.DMG) / 100f;
        if (raging(p)) amount *= 1.3f;
        float[] bh = BASTION_HIT.get(p.getUUID());
        if (bh != null) {
            BASTION_HIT.remove(p.getUUID());
            if (bh[0] > now) {
                amount += bh[1];
                p.serverLevel().sendParticles(ParticleTypes.FLASH, t.getX(), t.getY(0.6), t.getZ(), 1, 0, 0, 0, 0);
                p.serverLevel().sendParticles(new DustParticleOptions(new Vector3f(1f, 0.75f, 0.3f), 2f), t.getX(), t.getY(0.6), t.getZ(), 30, 0.5, 0.6, 0.5, 0);
                p.serverLevel().playSound(null, t.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1f, 0.6f);
                com.pvpciv.combat.DamageNumbers.show(p.serverLevel(), t, Component.literal("БАСТИОН").withStyle(x -> x.withColor(0xFFB040).withBold(true)));
            }
        }
        if (hidden(p)) endRetreat(p);
        double ls = stat(p, Stat.LIFESTEAL);
        if (ls > 0) p.heal((float) (amount * ls / 100));
        return amount;
    }

    /** Входящий урон игроку от врага (ближний бой и снаряды мобов/боссов, PvP). */
    public static float onDefend(ServerPlayer p, LivingEntity from, float amount) {
        StoryData d = Progress.d(p);
        if (cls(d) == null) return amount;
        double def = stat(p, Stat.DEF);
        if (def > 0) amount *= (float) (1 - Math.min(0.6, def / 100));
        if (raging(p)) amount *= 0.8f;
        float[] b = BASTION.get(p.getUUID());
        if (b != null && b[0] > now(p)) {
            float absorbed = amount * 0.8f;
            b[1] += absorbed;
            amount -= absorbed;
            p.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY(0.6), p.getZ(), 6, 0.4, 0.5, 0.4, 0.1);
        }
        return amount;
    }

    /** Игрок убил врага. */
    public static void onKill(ServerPlayer p, LivingEntity dead) {
        if (raging(p)) {
            long now = now(p), start = RAGE_START.getOrDefault(p.getUUID(), now);
            RAGE.put(p.getUUID(), Math.min(start + 280, RAGE.get(p.getUUID()) + 20));
        }
    }

    // ------------------------------------------------------------------ статы на атрибутах, реген, навыки во времени
    private static final ResourceLocation HP_ID = com.pvpciv.PvpCiv.id("cls_hp"), MS_ID = com.pvpciv.PvpCiv.id("cls_ms"),
            AS_ID = com.pvpciv.PvpCiv.id("cls_as"), KB_ID = com.pvpciv.PvpCiv.id("cls_kb");

    private static void mod(ServerPlayer p, net.minecraft.core.Holder<Attribute> a, ResourceLocation id, double v) {
        var inst = p.getAttribute(a);
        if (inst == null) return;
        var cur = inst.getModifier(id);
        if (Math.abs(v) < 1e-9) { if (cur != null) inst.removeModifier(id); return; }
        if (cur != null && Math.abs(cur.amount() - v) < 1e-9) return;
        inst.removeModifier(id);
        inst.addTransientModifier(new AttributeModifier(id, v, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static void applyStats(ServerPlayer p) {
        boolean bastion = BASTION.containsKey(p.getUUID()) && BASTION.get(p.getUUID())[0] > now(p);
        float hpFrac = p.getHealth() / Math.max(1f, p.getMaxHealth());
        double before = p.getMaxHealth();
        mod(p, Attributes.MAX_HEALTH, HP_ID, stat(p, Stat.HP) / 100);
        if (Math.abs(p.getMaxHealth() - before) > 0.01 && p.isAlive()) p.setHealth(Math.min(p.getMaxHealth(), hpFrac * p.getMaxHealth()));
        mod(p, Attributes.MOVEMENT_SPEED, MS_ID, stat(p, Stat.MSPD) / 100 - (bastion ? 0.15 : 0) - (POCKET.containsKey(p.getUUID()) ? 1.0 : 0));
        mod(p, Attributes.ATTACK_SPEED, AS_ID, stat(p, Stat.ASPD) / 100 + (raging(p) ? 0.10 : 0));
        var kb = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) {
            boolean want = raging(p), on = kb.getModifier(KB_ID) != null;
            if (want && !on) kb.addTransientModifier(new AttributeModifier(KB_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
            else if (!want && on) kb.removeModifier(KB_ID);
        }
    }

    public static void tick(MinecraftServer server) {
        int tc = server.getTickCount();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            pocketTick(p);
            UUID u = p.getUUID();
            long now = now(p);
            // Бастион: конец — заряд удара
            float[] b = BASTION.get(u);
            if (b != null) {
                if (b[0] <= now) {
                    BASTION.remove(u);
                    float cap = (float) (4 * com.pvpciv.combat.Melee.handDamage(p, p.getMainHandItem()));
                    float dmg = Math.min(cap, b[1]);
                    if (dmg > 0) {
                        BASTION_HIT.put(u, new float[]{now + 100, dmg});
                        p.displayClientMessage(Component.literal("Бастион: следующий удар +" + com.pvpciv.combat.Balance.fmt(dmg) + " (5 с)").withStyle(ChatFormatting.GOLD), true);
                    }
                    applyStats(p);
                } else if (tc % 4 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double a = (tc * 0.15) + i * Math.PI / 3;
                        p.serverLevel().sendParticles(new DustParticleOptions(new Vector3f(1f, 0.72f, 0.25f), 1.1f), p.getX() + Math.cos(a) * 0.9, p.getY() + 0.2 + (i % 3) * 0.6, p.getZ() + Math.sin(a) * 0.9, 1, 0, 0, 0, 0);
                    }
                }
            }
            // Ярость
            if (raging(p)) {
                p.removeEffect(ModRegistry.STUN);
                if (tc % 5 == 0) p.serverLevel().sendParticles(new DustParticleOptions(new Vector3f(1f, 0.15f, 0.1f), 1.3f), p.getX(), p.getY() + 1, p.getZ(), 4, 0.35, 0.6, 0.35, 0);
            } else if (RAGE.containsKey(u)) { RAGE.remove(u); RAGE_START.remove(u); applyStats(p); }
            // Отступление
            if (RETREAT.containsKey(u)) {
                if (!hidden(p)) endRetreat(p);
                else if (tc % 5 == 0) hideGear(p, true);
            }
        }
        if (tc % 10 == 0) for (ServerPlayer p : server.getPlayerList().getPlayers()) applyStats(p);
        if (tc % 20 == 0) for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            double r = stat(p, Stat.REGEN);
            if (r > 0 && p.isAlive() && p.getHealth() < p.getMaxHealth()) p.heal((float) (p.getMaxHealth() * r / 100));
        }
        if (tc % 40 == 0) for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            int n = com.pvpciv.inv.Bag.count(p, s -> s.is(com.pvpciv.registry.ModF5.DARK_MATTER.get()));
            if (n > 0) { com.pvpciv.inv.Bag.take(p, s -> s.is(com.pvpciv.registry.ModF5.DARK_MATTER.get()), n); giveMatter(p, n); }
        }
    }

    // ------------------------------------------------------------------ навыки слота 4
    public static void skill(ServerPlayer p, String id) {
        ServerLevel lvl = p.serverLevel();
        long now = now(p);
        switch (id) {
            case "bastion" -> {
                BASTION.put(p.getUUID(), new float[]{now + 140, 0});
                BASTION_HIT.remove(p.getUUID());
                applyStats(p);
                lvl.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1.2f, 0.6f);
                lvl.playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 0.7f);
                p.displayClientMessage(Component.literal("Бастион: 7 с — 80% урона поглощается").withStyle(ChatFormatting.GOLD), true);
            }
            case "rage" -> {
                RAGE.put(p.getUUID(), now + 160);
                RAGE_START.put(p.getUUID(), now);
                applyStats(p);
                lvl.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.2f, 1.2f);
                lvl.sendParticles(ParticleTypes.ANGRY_VILLAGER, p.getX(), p.getY() + 2, p.getZ(), 8, 0.6, 0.4, 0.6, 0);
                p.displayClientMessage(Component.literal("ЯРОСТЬ: +30% урона, −20% входящего").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
            }
            case "retreat" -> {
                lvl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX(), p.getY() + 0.3, p.getZ(), 10, 0.35, 0.2, 0.35, 0.01);
                lvl.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 0.8, p.getZ(), 14, 0.3, 0.5, 0.3, 0.01);
                lvl.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.4f, 1.6f);
                RETREAT.put(p.getUUID(), now + 60);
                NEXT_CRIT.add(p.getUUID());
                p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false, false));
                for (Mob m : lvl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(48), m -> m.getTarget() == p)) m.setTarget(null);
                hideGear(p, true);
            }
            default -> {}
        }
    }

    /** Мобам и боссам нельзя выбрать невидимого ассасина целью (вызывается из события смены цели). */
    public static boolean blockTarget(LivingEntity target) { return hidden(target); }

    private static void endRetreat(ServerPlayer p) {
        RETREAT.remove(p.getUUID());
        NEXT_CRIT.remove(p.getUUID());
        p.removeEffect(MobEffects.INVISIBILITY);
        hideGear(p, false);
    }

    /** Другим игрокам — пустая (hide) или настоящая экипировка. */
    private static void hideGear(ServerPlayer p, boolean hide) {
        List<com.mojang.datafixers.util.Pair<EquipmentSlot, ItemStack>> l = new ArrayList<>();
        for (EquipmentSlot s : EquipmentSlot.values()) if (s.getType() != EquipmentSlot.Type.ANIMAL_ARMOR)
            l.add(com.mojang.datafixers.util.Pair.of(s, hide ? ItemStack.EMPTY : p.getItemBySlot(s).copy()));
        p.serverLevel().getChunkSource().broadcast(p, new net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket(p.getId(), l));
    }

    // ------------------------------------------------------------------ Карманный телепорт (слот 3)
    private static final class Pocket { final Vec3 at; final long start; Pocket(Vec3 a, long s) { at = a; start = s; } }
    private static final Map<UUID, Pocket> POCKET = new HashMap<>();
    /** Каст завершён — окно врат открыто, можно выбрать точку (до тика). */
    private static final Map<UUID, Long> POCKET_READY = new HashMap<>();
    public static final int POCKET_CAST = 60, POCKET_CD = 180;

    public static boolean pocketCasting(ServerPlayer p) { return POCKET.containsKey(p.getUUID()); }

    /** true — каст начат (перезарядку ставим только после переноса). */
    public static boolean pocketStart(ServerPlayer p) {
        if (POCKET.containsKey(p.getUUID())) return false;
        String why = null;
        if (Vampire.jailed(p)) why = "из темницы не уйти";
        else if (com.pvpciv.floor.BossManager.inLockedArena(p.serverLevel(), p.position())) why = "арена босса запечатана";
        else if (VoidRift.inRift(p)) why = "Пустота не отпускает";
        else if (Gates.inCombat(p)) why = "тебя атакуют";
        else if (CivWorldFloor(p) < 1) why = "здесь телепорт не работает";
        else if (p.isPassenger()) why = "слезь с транспорта";
        if (why != null) { p.displayClientMessage(Component.literal("Карманный телепорт: " + why + ".").withStyle(ChatFormatting.RED), true); return false; }
        POCKET.put(p.getUUID(), new Pocket(p.position(), now(p)));
        POCKET_READY.remove(p.getUUID());
        com.pvpciv.combat.CombatEvents.noJump(p, POCKET_CAST + 5);
        applyStats(p);
        p.setDeltaMovement(Vec3.ZERO);
        pose(p, POCKET_CAST);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.9f, 1.5f);
        return true;
    }

    private static int CivWorldFloor(ServerPlayer p) { return com.pvpciv.world.CivWorld.is(p.level()) ? Math.max(1, com.pvpciv.world.CivWorld.floor(p.level())) : 0; }

    /** Поза «руки вверх» на клиентах (t — длительность в тиках, 0 — снять). */
    private static void pose(ServerPlayer p, int t) {
        var pkt = new Network.PhoneData(28, "{\"e\":" + p.getId() + ",\"t\":" + t + "}");
        PacketDistributor.sendToPlayer(p, pkt);
        PacketDistributor.sendToPlayersTrackingEntity(p, pkt);
    }

    /** Урон по игроку во время каста — каст сорван, перезарядка не тратится. */
    public static void pocketInterrupt(ServerPlayer p) {
        if (POCKET.remove(p.getUUID()) == null) return;
        pose(p, 0);
        applyStats(p);
        p.displayClientMessage(Component.literal("Карманный телепорт сорван: тебя ударили.").withStyle(ChatFormatting.RED), true);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.7f, 1.4f);
    }

    private static void pocketTick(ServerPlayer p) {
        Pocket k = POCKET.get(p.getUUID());
        if (k == null) return;
        long t = now(p) - k.start;
        ServerLevel l = p.serverLevel();
        if (!p.isAlive() || p.level() != l || p.position().distanceToSqr(k.at) > 2.5) { POCKET.remove(p.getUUID()); pose(p, 0); applyStats(p); return; }
        // фиолетовые шарики стягиваются к рукам
        double a = t * 0.35, r = 1.4 - t / (double) POCKET_CAST;
        for (int i = 0; i < 2; i++) {
            double aa = a + i * Math.PI;
            l.sendParticles(new DustParticleOptions(new Vector3f(0.65f, 0.25f, 1f), 1.4f), p.getX() + Math.cos(aa) * r, p.getY() + 0.3 + t / (double) POCKET_CAST * 2.0, p.getZ() + Math.sin(aa) * r, 1, 0, 0, 0, 0);
        }
        if (t >= POCKET_CAST) {
            POCKET.remove(p.getUUID());
            applyStats(p);
            POCKET_READY.put(p.getUUID(), now(p) + 600);
            l.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.3f);
            Gates.open(p, "pocket-" + CivWorldFloor(p));
        }
    }

    /** Окно врат «pocket:N» — после каста (30 с на выбор). */
    public static boolean pocketReady(ServerPlayer p) {
        Long until = POCKET_READY.get(p.getUUID());
        return until != null && until > now(p);
    }

    /** Перенос состоялся: шарики на старте, перезарядка. */
    public static void pocketUsed(ServerPlayer p) {
        POCKET_READY.remove(p.getUUID());
        pose(p, 0);
        purple(p.serverLevel(), p.position());
        Skills.setCd(p, 3, POCKET_CD);
    }

    public static void purple(ServerLevel l, Vec3 at) {
        for (int i = 0; i < 26; i++) {
            Vec3 o = new Vec3(l.random.nextGaussian(), l.random.nextGaussian() * 0.8 + 1, l.random.nextGaussian()).scale(0.6);
            l.sendParticles(new DustParticleOptions(new Vector3f(0.6f + l.random.nextFloat() * 0.2f, 0.2f, 1f), 2.2f), at.x + o.x, at.y + o.y, at.z + o.z, 1, 0, 0, 0, 0);
        }
        l.sendParticles(ParticleTypes.PORTAL, at.x, at.y + 1, at.z, 40, 0.4, 0.8, 0.4, 0.4);
        l.playSound(null, net.minecraft.core.BlockPos.containing(at), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.2f);
    }
}
