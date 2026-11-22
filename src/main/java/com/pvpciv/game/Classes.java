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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
 * 1.0 — классы (дерево профессий). Выбор класса и каждый узел стоят «тёмную материю» (первые прохождения испытаний Луны).
 * Узлы дают особые механики, а не цифры; эволюции (взаимоисключающие) — ещё и навык слота 3 (V).
 * Чужое оружие при выбранном классе: −35% урона и −15% скорости атаки. Оружие класса выпадает чаще.
 * Новый класс (маг, стрелок) — новый элемент PClass и его узлы в NODES: система от этого не меняется.
 */
public final class Classes {
    private Classes() {}

    public enum PClass {
        ASSASSIN("Ассасин", 0xB070FF, "Кинжалы и сабли. Криты, яд, исчезновения.", WClass.DAGGER, WClass.SABER),
        SWORDSMAN("Мечник", 0x6AC8FF, "Меч. Контрудары, рассечение, стойкость.", WClass.SWORD),
        BERSERK("Берсерк", 0xFF5A40, "Топор. Ярость от ран, раскол брони, жажда крови.", WClass.AXE),
        CRUSHER("Крушитель", 0xFFB040, "Молот. Оглушения, ударные волны, непоколебимость.", WClass.HAMMER);
        public final String title, desc; public final int color; public final Set<WClass> weapons;
        PClass(String t, int c, String d, WClass... w) { title = t; color = c; desc = d; weapons = EnumSet.copyOf(Arrays.asList(w)); }
        public static PClass of(String s) { for (PClass c : values()) if (c.name().equals(s)) return c; return null; }
    }

    /** evo — эволюция (из одной группы можно взять одну); needs — сколько базовых узлов класса нужно до неё. */
    public record Node(String id, PClass cls, String name, String desc, int cost, boolean evo, String skill) {}

    public static final List<Node> NODES = List.of(
            new Node("a_shadow", PClass.ASSASSIN, "Из тени", "Первый удар по врагу, который бьёт не тебя, — всегда крит.", 1, false, null),
            new Node("a_vanish", PClass.ASSASSIN, "Исчезновение", "Убийство: 2 с невидимости и скорость II.", 1, false, null),
            new Node("a_venom", PClass.ASSASSIN, "Ядовитые клинки", "Удары отравляют: цель получает +2% урона за стак (до 8 стаков, 4 с).", 1, false, null),
            new Node("a_shade", PClass.ASSASSIN, "Эволюция: Тень", "Кинжалы: удар в спину ×1.6. Навык V «Шаг в тень»: телепорт за спину цели, следующий удар — крит.", 2, true, "shadowstep"),
            new Node("a_dancer", PClass.ASSASSIN, "Эволюция: Танцор клинков", "Сабли: комбо до ×10 (только у Танцора). Навык V «Вихрь клинков»: 4 удара по всем вокруг.", 2, true, "bladestorm"),

            new Node("s_counter", PClass.SWORDSMAN, "Контрудар", "Пропустил удар — следующий твой удар в течение 1.5 с бьёт ×1.8.", 1, false, null),
            new Node("s_cleave", PClass.SWORDSMAN, "Рассечение", "Удар мечом задевает ещё одного врага рядом на 60% урона.", 1, false, null),
            new Node("s_resolve", PClass.SWORDSMAN, "Стойкость", "Здоровье ниже 30% — щит на 20% здоровья (раз в 60 с).", 1, false, null),
            new Node("s_guard", PClass.SWORDSMAN, "Эволюция: Страж", "С мечом в руке −25% урона в ближнем бою. Навык V «Небесный разрез»: рывок-разрез по линии, ×3 урона.", 2, true, "skycut"),
            new Node("s_duel", PClass.SWORDSMAN, "Эволюция: Дуэлянт", "Каждый 4-й удар по одной цели — крит ×2. Навык V «Выпад»: 4 с все удары — криты.", 2, true, "lunge"),

            new Node("b_rage", PClass.BERSERK, "Кровавая ярость", "+1% урона за каждые 2% потерянного здоровья (до +40%).", 1, false, null),
            new Node("b_shred", PClass.BERSERK, "Раскол брони", "Каждый удар: цель получает +5% урона (до 5 стаков, 5 с).", 1, false, null),
            new Node("b_thirst", PClass.BERSERK, "Жажда", "Убийство лечит 8% здоровья.", 1, false, null),
            new Node("b_exec", PClass.BERSERK, "Эволюция: Палач", "Цели ниже 25% здоровья — урон ×1.5. Навык V «Казнь»: прыжок к цели и удар ×4 (×8 по добыче ниже 25%).", 2, true, "execute"),
            new Node("b_chief", PClass.BERSERK, "Эволюция: Вождь", "Навык V «Боевой клич»: 8 с вампиризм 15% и +20% урона тебе и союзникам рядом.", 2, true, "warcry"),

            new Node("c_concuss", PClass.CRUSHER, "Сотрясение", "Каждый 3-й удар по цели оглушает её на 0.6 с.", 1, false, null),
            new Node("c_shock", PClass.CRUSHER, "Ударная волна", "Удар задевает всех в радиусе 2.5 блока на 40% урона.", 1, false, null),
            new Node("c_steady", PClass.CRUSHER, "Непоколебимость", "С молотом в руке тебя не отбросить.", 1, false, null),
            new Node("c_quake", PClass.CRUSHER, "Эволюция: Землетряс", "Навык V «Сокрушающий прыжок»: прыжок и удар о землю — ×3 урона в радиусе 5, оглушение 1 с.", 2, true, "groundslam"),
            new Node("c_titan", PClass.CRUSHER, "Эволюция: Титан", "Удар молотом отбрасывает врагов далеко. Навык V «Каменная кожа»: 5 с −60% урона.", 2, true, "stoneskin"));

    public static final int PICK_COST = 1, EVO_NEEDS = 2;
    public static final double OFF_DMG = 0.65, OFF_SPEED = -0.15;

    public static Node node(String id) { for (Node n : NODES) if (n.id.equals(id)) return n; return null; }
    public static PClass cls(StoryData d) { return PClass.of(d.gates.getString("cls")); }
    public static boolean has(StoryData d, String node) { return d.gates.getString("cls_n").contains("," + node + ","); }
    public static boolean has(ServerPlayer p, String node) { return has(Progress.d(p), node); }

    public static boolean classWeapon(PClass c, ItemStack st) { return c != null && st.getItem() instanceof CivWeaponItem w && c.weapons.contains(w.cls); }
    /** Чужое оружие при выбранном классе. */
    public static boolean offClass(ServerPlayer p, ItemStack st) {
        PClass c = cls(Progress.d(p));
        return c != null && st.getItem() instanceof CivWeaponItem w && !c.weapons.contains(w.cls);
    }
    /** Оружие класса — для перекоса лута. */
    public static Predicate<Item> prefer(ServerPlayer p) {
        PClass c = p == null ? null : cls(Progress.d(p));
        if (c == null) return null;
        return it -> it instanceof CivWeaponItem w && c.weapons.contains(w.cls);
    }

    // ------------------------------------------------------------------ тёмная материя
    /** 1.1: тёмная материя — баланс игрока (видно в навыках и в дереве), не предмет. */
    public static int matter(ServerPlayer p) { return Progress.d(p).gates.getInt("dm"); }
    public static void addMatter(StoryData d, int n) { d.gates.putInt("dm", Math.max(0, d.gates.getInt("dm") + n)); }
    public static void giveMatter(ServerPlayer p, int n) {
        StoryData d = Progress.d(p);
        addMatter(d, n);
        p.setData(ModRegistry.STORY.get(), d);
        p.displayClientMessage(Component.literal("+" + n + " тёмной материи (всего " + matter(p) + ")").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        sync(p, false);
    }
    private static boolean pay(ServerPlayer p, int n) {
        if (matter(p) < n) { p.displayClientMessage(Component.literal("Нужно тёмной материи: " + n + " (есть " + matter(p) + "). Её дают первые прохождения испытаний Луны.").withStyle(ChatFormatting.RED), true); return false; }
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
            case "pick" -> { if (a.length == 3) pick(p, a[2]); }
            case "node" -> { if (a.length == 3) learn(p, a[2]); }
            default -> {}
        }
    }

    public static void pick(ServerPlayer p, String id) {
        StoryData d = Progress.d(p);
        PClass c = PClass.of(id);
        if (c == null || cls(d) != null) return;
        if (!pay(p, PICK_COST)) { sync(p, false); return; }
        d = Progress.d(p);
        d.gates.putString("cls", c.name());
        d.gates.putString("cls_n", ",");
        p.setData(ModRegistry.STORY.get(), d);
        Story.title(p, c.title, "Класс выбран. Чужое оружие теперь слабее.", c.color);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.7f, 1.3f);
        com.pvpciv.progress.Titles.grant(p, "cls_" + c.name().toLowerCase(Locale.ROOT));
        sync(p, false);
    }

    public static void learn(ServerPlayer p, String id) {
        StoryData d = Progress.d(p);
        Node n = node(id);
        PClass c = cls(d);
        if (n == null || c == null || n.cls != c || has(d, id)) return;
        if (n.evo) {
            int base = 0;
            for (Node o : NODES) if (o.cls == c && !o.evo && has(d, o.id)) base++;
            if (base < EVO_NEEDS) { p.displayClientMessage(Component.literal("Эволюция откроется после " + EVO_NEEDS + " узлов класса.").withStyle(ChatFormatting.GRAY), true); return; }
            for (Node o : NODES) if (o.cls == c && o.evo && has(d, o.id)) { p.displayClientMessage(Component.literal("Эволюция уже выбрана. Сменить — свиток забвения.").withStyle(ChatFormatting.GRAY), true); return; }
        }
        if (!pay(p, n.cost)) { sync(p, false); return; }
        d = Progress.d(p);
        d.gates.putString("cls_n", d.gates.getString("cls_n") + id + ",");
        if (n.skill != null) { d.skill3 = n.skill; }
        p.setData(ModRegistry.STORY.get(), d);
        Story.title(p, n.name, n.evo ? "Новая эволюция!" : "Узел изучен", c.color);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.5f);
        if (n.skill != null) p.sendSystemMessage(Component.literal("✦ Навык слота 3 (V): " + Skills.byId(n.skill).name()).withStyle(ChatFormatting.LIGHT_PURPLE));
        sync(p, false);
        Skills.sync(p);
    }

    /** Свиток забвения: класс и узлы сброшены, вся потраченная тёмная материя возвращается. */
    public static boolean forget(ServerPlayer p) {
        StoryData d = Progress.d(p);
        if (cls(d) == null) { p.displayClientMessage(Component.literal("Нечего забывать: класс не выбран.").withStyle(ChatFormatting.GRAY), true); return false; }
        long now = System.currentTimeMillis(), next = d.gates.getLong("forget_at");
        if (now < next) {
            long h = (next - now) / 3_600_000, m = (next - now) / 60_000 % 60;
            p.displayClientMessage(Component.literal("Память ещё не остыла: свиток сработает через " + h + " ч " + m + " мин.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        int back = d.gates.getInt("dm_spent");
        d.gates.remove("cls"); d.gates.remove("cls_n"); d.gates.putInt("dm_spent", 0);
        d.gates.putLong("forget_at", now + 24L * 3_600_000);
        if (Skills.byId(d.skill3) != null) d.skill3 = "";
        p.setData(ModRegistry.STORY.get(), d);
        if (back > 0) { d = Progress.d(p); addMatter(d, back); p.setData(ModRegistry.STORY.get(), d); }
        Story.title(p, "Забвение", "Класс сброшен. Тёмная материя возвращена: " + back, 0xC0A0FF);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 0.5f);
        sync(p, false);
        Skills.sync(p);
        return true;
    }

    public static void sync(ServerPlayer p, boolean open) {
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
            o.addProperty("id", k.name()); o.addProperty("name", k.title); o.addProperty("desc", k.desc); o.addProperty("color", k.color);
            StringBuilder w = new StringBuilder();
            for (WClass wc : k.weapons) w.append(w.length() > 0 ? ", " : "").append(wc.title);
            o.addProperty("weapons", w.toString());
            cl.add(o);
        }
        j.add("classes", cl);
        JsonArray ns = new JsonArray();
        if (c != null) for (Node n : NODES) if (n.cls == c) {
            JsonObject o = new JsonObject();
            o.addProperty("id", n.id); o.addProperty("name", n.name); o.addProperty("desc", n.desc); o.addProperty("cost", n.cost); o.addProperty("evo", n.evo);
            o.addProperty("have", has(d, n.id));
            ns.add(o);
        }
        j.add("nodes", ns);
        PacketDistributor.sendToPlayer(p, new Network.PhoneData(23, j.toString()));
    }

    // ------------------------------------------------------------------ бой
    public static int comboMax(ServerPlayer p) {
        return has(p, "a_dancer") && p.getMainHandItem().getItem() instanceof CivWeaponItem w && w.cls == WClass.SABER ? 10 : com.pvpciv.combat.Melee.COMBO_MAX;
    }

    private record Stack(int n, long until) {}
    private static final Map<UUID, Stack> VENOM = new HashMap<>(), SHRED = new HashMap<>();
    private static final Map<String, Integer> HITS = new HashMap<>();   // игрок:цель → счётчик ударов
    private static final Map<UUID, Long> COUNTER = new HashMap<>(), RESOLVE_CD = new HashMap<>(), FOCUS = new HashMap<>(), STONE = new HashMap<>(), WARCRY = new HashMap<>();
    private static final Set<UUID> NEXT_CRIT = new HashSet<>();
    private static final Set<String> FIRST = new HashSet<>();

    private static long now(ServerPlayer p) { return p.serverLevel().getGameTime(); }

    /** Гарантированный крит этого удара (Из тени, Шаг в тень, Выпад, Дуэлянт). */
    public static boolean forceCrit(ServerPlayer p, LivingEntity t) {
        if (NEXT_CRIT.remove(p.getUUID())) return true;
        if (FOCUS.getOrDefault(p.getUUID(), 0L) > now(p)) return true;
        ItemStack st = com.pvpciv.combat.Melee.striking(p);
        if (has(p, "a_shadow") && classWeapon(PClass.ASSASSIN, st) && FIRST.add(p.getUUID() + ":" + t.getUUID())
                && !(t instanceof Mob m && m.getTarget() == p)) return true;
        return false;
    }

    /** Удар игрока (после уровней и критов). */
    public static float onAttack(ServerPlayer p, LivingEntity t, float amount, boolean melee) {
        if (!melee) return amount;
        StoryData d = Progress.d(p);
        PClass c = cls(d);
        // на цели висит яд/раскол от кого угодно
        long now = now(p);
        Stack v = VENOM.get(t.getUUID()), s = SHRED.get(t.getUUID());
        if (v != null && v.until > now) amount *= 1 + 0.02f * v.n;
        if (s != null && s.until > now) amount *= 1 + 0.05f * s.n;
        if (WARCRY.getOrDefault(p.getUUID(), 0L) > now) amount *= 1.2f;
        if (c == null) return amount;
        ItemStack st = com.pvpciv.combat.Melee.striking(p);
        if (!(st.getItem() instanceof CivWeaponItem w)) return amount;
        if (!c.weapons.contains(w.cls)) return (float) (amount * OFF_DMG);
        String key = p.getUUID() + ":" + t.getUUID();
        int hits = HITS.merge(key, 1, Integer::sum);
        if (HITS.size() > 4000) HITS.clear();
        ServerLevel lvl = p.serverLevel();
        switch (c) {
            case ASSASSIN -> {
                if (has(d, "a_venom")) { VENOM.put(t.getUUID(), new Stack(Math.min(8, (v != null && v.until > now ? v.n : 0) + 1), now + 80)); lvl.sendParticles(ParticleTypes.ITEM_SLIME, t.getX(), t.getY(0.6), t.getZ(), 3, 0.2, 0.2, 0.2, 0); }
                if (has(d, "a_shade") && w.cls == WClass.DAGGER) {
                    Vec3 look = t.getLookAngle().multiply(1, 0, 1), to = p.position().subtract(t.position()).multiply(1, 0, 1);
                    if (look.lengthSqr() > 1e-4 && to.lengthSqr() > 1e-4 && look.normalize().dot(to.normalize()) < -0.3) {
                        amount *= 1.6f;
                        com.pvpciv.combat.DamageNumbers.show(lvl, t, Component.literal("В СПИНУ").withStyle(x -> x.withColor(0xB070FF).withBold(true)));
                    }
                }
            }
            case SWORDSMAN -> {
                if (has(d, "s_counter") && COUNTER.getOrDefault(p.getUUID(), 0L) > now) { amount *= 1.8f; COUNTER.remove(p.getUUID()); com.pvpciv.combat.DamageNumbers.show(lvl, t, Component.literal("КОНТРУДАР").withStyle(x -> x.withColor(0x6AC8FF).withBold(true))); }
                if (has(d, "s_duel") && hits % 4 == 0) { amount *= 2f; com.pvpciv.combat.DamageNumbers.show(lvl, t, Component.literal("ДУЭЛЬ ×2").withStyle(x -> x.withColor(0x6AC8FF).withBold(true))); }
                if (has(d, "s_cleave") && !com.pvpciv.combat.Melee.isSweeping()) splash(p, t, amount * 0.6f, 3.0, 1);
            }
            case BERSERK -> {
                if (has(d, "b_rage")) amount *= 1 + Math.min(0.4f, (1 - p.getHealth() / p.getMaxHealth()) * 0.5f);
                if (has(d, "b_exec") && t.getHealth() < t.getMaxHealth() * 0.25f) amount *= 1.5f;
                if (has(d, "b_shred")) SHRED.put(t.getUUID(), new Stack(Math.min(5, (s != null && s.until > now ? s.n : 0) + 1), now + 100));
                if (WARCRY.getOrDefault(p.getUUID(), 0L) > now) p.heal(amount * 0.15f);
            }
            case CRUSHER -> {
                if (has(d, "c_concuss") && hits % 3 == 0 && !com.pvpciv.floor.BossManager.isBoss(t)) t.addEffect(new MobEffectInstance(ModRegistry.STUN, 12, 0));
                if (has(d, "c_shock") && !com.pvpciv.combat.Melee.isSweeping()) splash(p, t, amount * 0.4f, 2.5, 8);
                if (has(d, "c_titan") && !com.pvpciv.floor.BossManager.isBoss(t)) {
                    Vec3 k = t.position().subtract(p.position()).multiply(1, 0, 1).normalize().scale(1.4);
                    t.push(k.x, 0.35, k.z); t.hurtMarked = true;
                }
            }
        }
        return amount;
    }

    private static boolean splashing;
    /** Урон по другим врагам рядом с целью. */
    private static void splash(ServerPlayer p, LivingEntity center, float dmg, double r, int max) {
        if (splashing) return;
        splashing = true;
        try {
            int n = 0;
            for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, center.getBoundingBox().inflate(r), x -> x != center && x != p && x.isAlive()
                    && (x instanceof net.minecraft.world.entity.monster.Enemy || com.pvpciv.combat.Mobs.level(x) > 0) && !(x instanceof ServerPlayer))) {
                if (n++ >= max) break;
                e.invulnerableTime = 0;
                e.hurt(p.damageSources().playerAttack(p), dmg);
            }
            if (n > 0) p.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK, center.getX(), center.getY(0.5), center.getZ(), 2, 0.6, 0.1, 0.6, 0);
        } finally { splashing = false; }
    }

    /** Входящий урон игроку от врага в ближнем бою. */
    public static float onDefend(ServerPlayer p, LivingEntity from, float amount) {
        StoryData d = Progress.d(p);
        if (cls(d) == null) return amount;
        long now = now(p);
        if (STONE.getOrDefault(p.getUUID(), 0L) > now) amount *= 0.4f;
        if (has(d, "s_guard") && classWeapon(PClass.SWORDSMAN, p.getMainHandItem())) amount *= 0.75f;
        if (has(d, "s_counter")) COUNTER.put(p.getUUID(), now + 30);
        if (has(d, "s_resolve") && (p.getHealth() - amount) < p.getMaxHealth() * 0.3f && RESOLVE_CD.getOrDefault(p.getUUID(), 0L) <= now) {
            RESOLVE_CD.put(p.getUUID(), now + 1200);
            p.setAbsorptionAmount(p.getAbsorptionAmount() + p.getMaxHealth() * 0.2f);
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 0.8f);
            p.displayClientMessage(Component.literal("Стойкость: щит!").withStyle(ChatFormatting.AQUA), true);
        }
        return amount;
    }

    /** Игрок убил врага. */
    public static void onKill(ServerPlayer p, LivingEntity dead) {
        StoryData d = Progress.d(p);
        if (cls(d) == null) return;
        if (has(d, "a_vanish") && classWeapon(PClass.ASSASSIN, p.getMainHandItem())) {
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
            p.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 15, 0.3, 0.5, 0.3, 0.01);
        }
        if (has(d, "b_thirst") && classWeapon(PClass.BERSERK, p.getMainHandItem())) p.heal(p.getMaxHealth() * 0.08f);
        if (has(d, "b_exec")) CD_RESET.add(p.getUUID());
        FIRST.remove(p.getUUID() + ":" + dead.getUUID());
    }
    public static final Set<UUID> CD_RESET = new HashSet<>();

    // ------------------------------------------------------------------ скорость атаки чужим оружием, непоколебимость
    private static final ResourceLocation OFF_ID = com.pvpciv.PvpCiv.id("off_class"), STEADY_ID = com.pvpciv.PvpCiv.id("cls_steady");

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 10 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            boolean off = offClass(p, p.getMainHandItem());
            var sp = p.getAttribute(Attributes.ATTACK_SPEED);
            if (sp != null) {
                boolean on = sp.getModifier(OFF_ID) != null;
                if (off && !on) sp.addTransientModifier(new AttributeModifier(OFF_ID, OFF_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                else if (!off && on) sp.removeModifier(OFF_ID);
            }
            boolean steady = has(p, "c_steady") && classWeapon(PClass.CRUSHER, p.getMainHandItem());
            var kb = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (kb != null) {
                boolean on = kb.getModifier(STEADY_ID) != null;
                if (steady && !on) kb.addTransientModifier(new AttributeModifier(STEADY_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
                else if (!steady && on) kb.removeModifier(STEADY_ID);
            }
        }
        if (server.getTickCount() % 40 == 0) for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            int n = com.pvpciv.inv.Bag.count(p, s -> s.is(com.pvpciv.registry.ModF5.DARK_MATTER.get()));
            if (n > 0) { com.pvpciv.inv.Bag.take(p, s -> s.is(com.pvpciv.registry.ModF5.DARK_MATTER.get()), n); giveMatter(p, n); }
        }
        long t = server.overworld().getGameTime();
        VENOM.values().removeIf(s -> s.until < t - 200);
        SHRED.values().removeIf(s -> s.until < t - 200);
    }

    // ------------------------------------------------------------------ навыки слота 3 (V)
    public static void skill(ServerPlayer p, String id) {
        ServerLevel lvl = p.serverLevel();
        long now = now(p);
        LivingEntity tgt = aim(p, 12);
        double base = com.pvpciv.combat.Melee.handDamage(p, p.getMainHandItem());
        switch (id) {
            case "shadowstep" -> {
                if (tgt == null) { p.displayClientMessage(Component.literal("Нет цели перед тобой").withStyle(ChatFormatting.GRAY), true); return; }
                Vec3 back = tgt.position().subtract(tgt.getLookAngle().multiply(1, 0, 1).normalize().scale(1.3));
                lvl.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 20, 0.3, 0.6, 0.3, 0.01);
                p.teleportTo(back.x, tgt.getY(), back.z);
                p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, tgt.getEyePosition());
                p.connection.teleport(p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());
                NEXT_CRIT.add(p.getUUID());
                lvl.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.4f);
            }
            case "bladestorm" -> {
                for (int i = 0; i < 4; i++) GameEvents.later(p.server, i * 8, () -> {
                    if (!p.isAlive()) return;
                    for (LivingEntity e : enemies(p, 3.8)) { e.invulnerableTime = 0; e.hurt(p.damageSources().playerAttack(p), (float) (base * 1.1)); }
                    for (int k = 0; k < 10; k++) { double a = Math.PI * 2 * k / 10; lvl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + Math.cos(a) * 2.2, p.getY(0.5), p.getZ() + Math.sin(a) * 2.2, 1, 0, 0, 0, 0); }
                    lvl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.3f);
                });
            }
            case "skycut" -> {
                Vec3 dir = p.getLookAngle().multiply(1, 0, 1).normalize(), from = p.position();
                Vec3 to = from.add(dir.scale(8));
                Vec3 safe = com.pvpciv.game.Taxi.safe(lvl, net.minecraft.core.BlockPos.containing(to.x, p.getY(), to.z));
                if (Math.abs(safe.y - p.getY()) > 2) safe = from.add(dir.scale(3));
                for (LivingEntity e : lvl.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(from, to).inflate(1.5, 2, 1.5), x -> x != p && x.isAlive() && !(x instanceof ServerPlayer)
                        && (x instanceof net.minecraft.world.entity.monster.Enemy || com.pvpciv.combat.Mobs.level(x) > 0))) {
                    e.invulnerableTime = 0; e.hurt(p.damageSources().playerAttack(p), (float) (base * 3));
                }
                for (int i = 0; i <= 16; i++) { Vec3 q = from.add(dir.scale(i * 0.5)); lvl.sendParticles(new DustParticleOptions(new Vector3f(0.5f, 0.85f, 1f), 1.6f), q.x, q.y + 1, q.z, 3, 0.1, 0.3, 0.1, 0); }
                p.teleportTo(safe.x, safe.y, safe.z);
                lvl.playSound(null, p.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1f, 1.2f);
            }
            case "lunge" -> { FOCUS.put(p.getUUID(), now + 80); lvl.playSound(null, p.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1f, 1.5f); p.displayClientMessage(Component.literal("Выпад: 4 с — только криты").withStyle(ChatFormatting.AQUA), true); }
            case "execute" -> {
                if (tgt == null) { p.displayClientMessage(Component.literal("Нет цели перед тобой").withStyle(ChatFormatting.GRAY), true); return; }
                Vec3 to = tgt.position().add(p.position().subtract(tgt.position()).normalize().scale(1.2));
                p.teleportTo(to.x, tgt.getY(), to.z);
                boolean low = tgt.getHealth() < tgt.getMaxHealth() * 0.25f;
                tgt.invulnerableTime = 0;
                tgt.hurt(p.damageSources().playerAttack(p), (float) (base * (low ? 8 : 4)));
                lvl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, tgt.getX(), tgt.getY(0.6), tgt.getZ(), 20, 0.4, 0.4, 0.4, 0.2);
                lvl.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1f, 0.6f);
            }
            case "warcry" -> {
                for (ServerPlayer o : lvl.players()) if (o.distanceTo(p) < 10) {
                    WARCRY.put(o.getUUID(), now + 160);
                    o.displayClientMessage(Component.literal("Боевой клич " + p.getGameProfile().getName() + ": +20% урона, вампиризм").withStyle(ChatFormatting.RED), true);
                }
                lvl.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.2f, 1.1f);
                lvl.sendParticles(ParticleTypes.ANGRY_VILLAGER, p.getX(), p.getY() + 2, p.getZ(), 10, 1, 0.5, 1, 0);
            }
            case "groundslam" -> {
                p.setDeltaMovement(p.getLookAngle().multiply(0.6, 0, 0.6).add(0, 0.9, 0));
                p.hurtMarked = true;
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
                GameEvents.later(p.server, 16, () -> {
                    if (!p.isAlive()) return;
                    for (LivingEntity e : enemies(p, 5)) {
                        e.invulnerableTime = 0; e.hurt(p.damageSources().playerAttack(p), (float) (base * 3));
                        if (!com.pvpciv.floor.BossManager.isBoss(e)) e.addEffect(new MobEffectInstance(ModRegistry.STUN, 20, 0));
                    }
                    lvl.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY(), p.getZ(), 6, 2, 0.2, 2, 0);
                    lvl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX(), p.getY(), p.getZ(), 30, 2.5, 0.2, 2.5, 0.02);
                    lvl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1f, 0.7f);
                    p.resetFallDistance();
                });
            }
            case "stoneskin" -> { STONE.put(p.getUUID(), now + 100); lvl.playSound(null, p.blockPosition(), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.5f, 0.5f); p.displayClientMessage(Component.literal("Каменная кожа: −60% урона на 5 с").withStyle(ChatFormatting.GOLD), true); }
            default -> {}
        }
    }

    private static List<LivingEntity> enemies(ServerPlayer p, double r) {
        return p.serverLevel().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), x -> x != p && x.isAlive() && !(x instanceof ServerPlayer)
                && (x instanceof net.minecraft.world.entity.monster.Enemy || com.pvpciv.combat.Mobs.level(x) > 0) && x.distanceToSqr(p) <= r * r);
    }

    private static LivingEntity aim(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        LivingEntity best = null; double bd = 1e9;
        for (LivingEntity e : enemies(p, range)) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            double along = to.dot(look);
            if (along <= 0) continue;
            double off = to.subtract(look.scale(along)).length();
            if (off < 2.0 && along < bd) { bd = along; best = e; }
        }
        return best;
    }
}
