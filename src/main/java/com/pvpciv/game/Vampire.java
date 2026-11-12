package com.pvpciv.game;

import com.pvpciv.combat.Mobs;
import com.pvpciv.data.StoryData;
import com.pvpciv.entity.CarmillaEntity;
import com.pvpciv.game.DialogueManager.Dialogue;
import com.pvpciv.net.Network;
import com.pvpciv.progress.Progress;
import com.pvpciv.progress.Titles;
import com.pvpciv.registry.ModRegistry;
import com.pvpciv.world.Basement;
import com.pvpciv.world.CivWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static com.pvpciv.game.DialogueManager.opt;

/**
 * Кровавые контракты Кармиллы. Берёшь контракт — убиваешь мобов за 20 минут — сдаёшь в подвале и получаешь Искры,
 * звёзды (на сложных — благословенные). Не успел или отказался — Кармилла сама приходит за тобой,
 * уводит в подвал (катсцена), кусает и запирает в клетке на 3 минуты. Первый раз — титул «Жертва вампиризма».
 */
public final class Vampire {
    public static final String NAME = "Кармилла";
    public static final int C = CarmillaEntity.COLOR;
    public static final long TIME_LIMIT = 20L * 60 * 20, COOLDOWN_DONE = 20L * 60 * 45, COOLDOWN_FAIL = 20L * 60 * 30, JAIL = 20L * 60 * 3;

    /** Как назвать N мобов вида kind в контракте. */
    private static String what(String kind) {
        return switch (kind) {
            case "ASH_ZOMBIE" -> "пепельных зомби"; case "WITHERED" -> "иссохших"; case "ASH_ARCHER" -> "пепельных лучников";
            case "ASH_SPIDER" -> "пепельных пауков"; case "FROST_ZOMBIE" -> "обмороженных"; case "FROST_ARCHER" -> "инеевых стрелков";
            case "FROST_SPIDER" -> "ледяных пауков"; case "ELITE" -> "элитных мобов (★)"; default -> kind;
        };
    }

    /** Позиция контракта: вид, сколько нужно, сколько убито. Хранится строкой «KIND:need:have;…» в questKind. */
    public record Pos(String kind, int need, int have) {}

    public static List<Pos> parse(StoryData d) {
        List<Pos> out = new ArrayList<>();
        for (String part : d.questKind.split(";")) {
            String[] f = part.split(":");
            if (f.length == 3) out.add(new Pos(f[0], Integer.parseInt(f[1]), Integer.parseInt(f[2])));
        }
        return out;
    }

    private static void store(StoryData d, List<Pos> list) {
        StringBuilder sb = new StringBuilder();
        int need = 0, have = 0;
        for (Pos x : list) { if (sb.length() > 0) sb.append(';'); sb.append(x.kind()).append(':').append(x.need()).append(':').append(x.have()); need += x.need(); have += Math.min(x.have(), x.need()); }
        d.questKind = sb.toString(); d.questNeed = need; d.questHave = have;
    }

    private static final String[] TIER = {"Лёгкий", "Средний", "Сложный", "Кровавый", "ЭЛИТНЫЙ"};
    private static final int[] TIER_COLOR = {0xB8B8B8, 0x6FD3FF, 0xC77DFF, 0xFF5566, 0xFFD166};
    public static final long ELITE_CD = 20L * 60 * 60 * 8;

    /** Реальное время в тиках (50 мс): кулдауны и смена предложений идут, даже когда сервер выключен (бета 12.11). */
    private static long now(MinecraftServer s) { return real(); }

    // ------------------------------------------------------------------ состояние
    /** Заточение считается по реальному времени: вышел из игры и зашёл — срок всё равно идёт. */
    private static long real() { return System.currentTimeMillis() / 50; }

    public static boolean jailed(ServerPlayer p) { return Progress.d(p).jailUntil > real(); }
    public static boolean hasQuest(StoryData d) { return !d.questKind.isEmpty(); }
    public static boolean questDone(StoryData d) { return hasQuest(d) && d.questHave >= d.questNeed; }

    /** Строки для HUD-трекера. */
    public static String hudLine(StoryData d) {
        if (!hasQuest(d)) return "";
        if (questDone(d)) return "Контракт выполнен — вернись к Кармилле";
        StringBuilder sb = new StringBuilder("Контракт:");
        for (Pos x : parse(d)) sb.append(" ").append(Math.min(x.have(), x.need())).append("/").append(x.need()).append(" ").append(what(x.kind())).append(";");
        return sb.substring(0, sb.length() - 1);
    }

    public static int hudSeconds(ServerPlayer p, StoryData d) {
        if (!hasQuest(d) || questDone(d)) return -1;
        return (int) Math.max(0, (d.questDeadline - now(p.server)) / 20);
    }

    public static int jailSeconds(ServerPlayer p) {
        long left = Progress.d(p).jailUntil - real();
        return left > 0 ? (int) (left / 20) : -1;
    }

    /** Виды мобов, которых игрок уже убивал (контракт — только на знакомых). */
    private static List<String> known(StoryData d) {
        List<String> out = new ArrayList<>();
        for (Mobs.Kind k : Mobs.Kind.values()) if (d.kills.getInt(k.name()) > 0) out.add(k.name());
        return out;
    }

    /** Как часто вид встречается в зонах спавна (сумма «мест» по всем зонам) — доли контракта по ней. */
    private static double share(Mobs.Kind k) {
        double s = 0;
        for (var z : com.pvpciv.floor.SpawnZones.zones()) {
            int c = 0;
            for (Mobs.Kind x : z.kinds()) if (x == k) c++;
            if (c > 0) s += z.max() * (double) c / z.kinds().length;
        }
        return Math.max(0.5, s);
    }

    /**
     * Предложение: 2–4 вида из одной части этажа (обычно — самой дальней, где игрок уже был), всего 16–50 мобов.
     * Сколько каких — пропорционально тому, как часто они спавнятся (крыс много — их и больше в контракте). Меняется раз в 10 минут.
     */
    private static List<Pos> offerFor(ServerPlayer p) {
        StoryData d = Progress.d(p);
        var r = net.minecraft.util.RandomSource.create(p.getUUID().getLeastSignificantBits() ^ (now(p.server) / (20L * 60 * 10)));
        List<Mobs.Kind> known = new ArrayList<>();
        for (Mobs.Kind k : Mobs.Kind.values()) if (d.kills.getInt(k.name()) > 0) known.add(k);
        Mobs.Kind top = known.stream().max(java.util.Comparator.comparingInt(k -> k.defLevel)).orElse(Mobs.Kind.ASH_ZOMBIE);
        String group = r.nextFloat() < 0.7f ? top.loot : known.get(r.nextInt(known.size())).loot;
        List<Mobs.Kind> kinds = new ArrayList<>(known.stream().filter(k -> k.loot.equals(group)).toList());
        java.util.Collections.shuffle(kinds, new java.util.Random(r.nextLong()));
        int n = kinds.size() <= 1 ? 1 : 2 + r.nextInt(Math.min(3, kinds.size() - 1));
        kinds = kinds.subList(0, Math.min(n, kinds.size()));
        int total = 16 + r.nextInt(8) * 4 + Math.min(16, d.questsDone * 2);
        total = Math.min(50, Math.max(total, kinds.size() * 4));
        double sum = 0;
        for (Mobs.Kind k : kinds) sum += share(k);
        List<Pos> out = new ArrayList<>();
        int left = total;
        for (int i = 0; i < kinds.size(); i++) {
            int part = i == kinds.size() - 1 ? left : Math.max(3, (int) Math.round(total * share(kinds.get(i)) / sum));
            part = Math.max(3, Math.min(part, left - 3 * (kinds.size() - 1 - i)));
            left -= part;
            out.add(new Pos(kinds.get(i).name(), part, 0));
        }
        return out;
    }

    private static int tierOf(List<Pos> c) { return c.size() >= 4 ? 3 : c.size() - 1; }

    private static String rewardText(List<Pos> c, boolean elite) {
        if (elite) return "500 ✦, 2 благословенные звезды и 40% на звезду крови";
        int n = c.size(), total = c.stream().mapToInt(Pos::need).sum();
        return (60 + total * 6 + n * 40) + " ✦, звёзды ×" + (1 + n / 2) + ", шанс " + (n * 12) + "% на благословенную";
    }

    private static String describe(List<Pos> c) {
        StringBuilder sb = new StringBuilder();
        for (Pos x : c) { if (sb.length() > 0) sb.append(", "); sb.append(x.need()).append(" ").append(what(x.kind())); }
        return sb.toString();
    }

    // ------------------------------------------------------------------ события
    public static void onKill(ServerPlayer p, Mob mob, boolean elite) {
        StoryData d = Progress.d(p);
        Mobs.Kind k = Mobs.Kind.of(mob.getType());
        if (k != null) d.kills.putInt(k.name(), d.kills.getInt(k.name()) + 1);
        p.setData(ModRegistry.STORY.get(), d);
        if (!hasQuest(d) || questDone(d)) return;
        List<Pos> list = parse(d);
        boolean hit = false;
        for (int i = 0; i < list.size(); i++) {
            Pos x = list.get(i);
            boolean counts = x.kind().equals("ELITE") ? elite : k != null && k.name().equals(x.kind());
            if (counts && x.have() < x.need()) { list.set(i, new Pos(x.kind(), x.need(), x.have() + 1)); hit = true; break; }
        }
        if (!hit) return;
        store(d, list);
        p.setData(ModRegistry.STORY.get(), d);
        if (questDone(d)) {
            d.questDeadline = 0;
            Story.title(p, "КОНТРАКТ ВЫПОЛНЕН", "Вернись в подвал к Кармилле", C);
            p.level().playSound(null, p.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1f, 1.2f);
        } else p.displayClientMessage(Component.literal("Контракт: " + d.questHave + "/" + d.questNeed).withStyle(s2 -> s2.withColor(C)), true);
        Story.sync(p);
    }

    /** Раз в секунду: сроки контракта и заточение. */
    public static void tick(ServerPlayer p) {
        if (!CivWorld.is(p.level())) return;
        StoryData d = Progress.d(p);
        long now = now(p.server);
        if (d.jailUntil > 0) {
            if (real() >= d.jailUntil) release(p);
            else if (!Basement.inCell(p.position()) && !ABDUCTING.contains(p.getUUID())) toCell(p);
            return;
        }
        if (hasQuest(d) && !questDone(d) && d.questDeadline > 0 && now >= d.questDeadline && !ABDUCTING.contains(p.getUUID())) {
            Story.say(p, NAME, C, "Время вышло, милый. Долг платежом красен — особенно кровью.");
            abduct(p);
        }
    }

    // ------------------------------------------------------------------ диалог
    public static Dialogue dialogue(ServerPlayer p, CarmillaEntity c) {
        StoryData d = Progress.d(p);
        long now = now(p.server);
        Dialogue dl = new Dialogue("a");
        if (jailed(p)) {
            return dl.node("a", NAME, C, "Тсс. Ты отбываешь долг. Посиди, подумай о сроках. Мне нравится, как ты думаешь — громко.", opt("[Молчать]", null));
        }
        if (questDone(d)) {
            return dl.node("a", NAME, C, "Учуяла тебя ещё с лестницы. " + d.questNeed + " голов — пахнет честной работой. Держи плату.",
                    pp -> reward(pp), opt("[Забрать награду]", null));
        }
        if (hasQuest(d)) {
            int sec = hudSeconds(p, d);
            return dl.node("a", NAME, C, String.format("Контракт ещё не закрыт: %d/%d. У тебя %d:%02d. Я считаю каждую секунду — у меня их много.", d.questHave, d.questNeed, sec / 60, sec % 60),
                            opt("Работаю!", null), opt("Я передумал… (отказаться)", "quit"))
                    .node("quit", NAME, C, "Передумал? Как мило. Тогда заплатишь сейчас. Не бойся — больно только первые пару секунд.",
                            opt("Подожди, я…", "quit2"))
                    .node("quit2", NAME, C, "…", pp -> { clearQuest(pp); abduct(pp); }, opt("[…]", null));
        }
        if (known(d).isEmpty()) {
            return dl.node("a", NAME, C, "Новенький? Пахнешь общежитием, а не кровью. Сначала докажи, что умеешь убивать: сходи на этаж, поохоться — тогда и поговорим о контрактах.",
                    opt("[Уйти]", null));
        }
        boolean eliteOpen = d.questsDone >= 5 && d.eliteCd <= now;
        if (d.questCooldown > now && !eliteOpen) {
            long min = (d.questCooldown - now) / 1200 + 1;
            return dl.node("a", NAME, C, d.cdFail
                    ? "Свой долг ты уже отсидел. Новый контракт — через " + min + " мин. Ешь гранаты, у тебя бледный вид."
                    : "Ты сегодня уже был мне полезен. Приходи через " + min + " мин. И береги шею.", opt("[Уйти]", null));
        }
        List<Pos> k = offerFor(p);
        List<Pos> ek = List.of(new Pos("ELITE", 5, 0));
        boolean first = d.questsDone == 0 && d.jailed == 0;
        String intro = first
                ? "Новенький? В подвал общежития студенты обычно не спускаются. А те, кто спускается… становятся моими должниками. Шучу. Наполовину."
                : "Снова ты. Соскучился по подвалу? Или по мне?";
        List<DialogueManager.Opt> opts = new ArrayList<>();
        opts.add(opt("Кто ты такая?", first ? "who" : "offer"));
        if (d.questCooldown <= now) opts.add(opt("Есть работа?", "offer"));
        if (eliteOpen) opts.add(opt("★ Элитный контракт", "elite"));
        opts.add(opt("[Уйти]", null));
        dl.node("a", NAME, C, intro, opts.toArray(new DialogueManager.Opt[0]));
        dl.node("who", NAME, C, "Кармилла. Этот подвал был моим склепом задолго до того, как сверху построили ваше общежитие. Комендант Агафья делает вид, что меня нет. Я делаю вид, что не голодна.",
                opt("И что тебе нужно?", "deal"));
        dl.node("deal", NAME, C, "Мне скучно, а мобы на этажах мешают моим… поставкам. Я даю контракт — ты чистишь этаж и получаешь плату. Не успел — плату беру я. Кровью. Немного. Обычно.",
                opt("Звучит честно…?", d.questCooldown <= now ? "offer" : "elite"));
        dl.node("offer", NAME, C, "Контракт «" + TIER[tierOf(k)] + "»: " + describe(k) + " за 20 минут. Плата: " + rewardText(k, false)
                        + ". Не успеешь — я приду за тобой сама.",
                opt("Подписываю кровью", "take"), opt("Не сегодня", "no"));
        dl.node("take", NAME, C, "Ммм, хороший почерк. Беги. Часы уже тикают — я их слышу.", pp -> take(pp, k, false), opt("[Уйти]", null));
        dl.node("elite", NAME, C, "Для своих у меня особая работа: 5 элитных мобов (★) за 25 минут. Плата: " + rewardText(ek, true)
                        + ". Провалишь — посидишь у меня 10 минут. Раз в 8 часов.",
                opt("Подписываю кровью", "etake"), opt("Не сегодня", "no"));
        dl.node("etake", NAME, C, "Вот это я понимаю — аппетит. Иди.", pp -> take(pp, ek, true), opt("[Уйти]", null));
        dl.node("no", NAME, C, "Как скажешь. Предложение меняется каждые десять минут — как и моё настроение. Кстати, если увидишь блондинку с арбалетом на третьем этаже — передай, что я соскучилась.", opt("[Уйти]", null));
        return dl;
    }

    private static void take(ServerPlayer p, List<Pos> k, boolean elite) {
        StoryData d = Progress.d(p);
        store(d, k);
        d.questTier = elite ? 4 : tierOf(k);
        d.questDeadline = now(p.server) + (elite ? TIME_LIMIT * 5 / 4 : TIME_LIMIT);
        if (elite) d.eliteCd = now(p.server) + ELITE_CD;
        p.setData(ModRegistry.STORY.get(), d);
        Story.title(p, elite ? "ЭЛИТНЫЙ КОНТРАКТ" : "КОНТРАКТ", d.questNeed + " целей • " + (elite ? "25:00" : "20:00"), TIER_COLOR[d.questTier]);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.8f);
        Story.sync(p);
    }

    private static void clearQuest(ServerPlayer p) {
        StoryData d = Progress.d(p);
        d.questKind = ""; d.questNeed = 0; d.questHave = 0; d.questDeadline = 0;
        p.setData(ModRegistry.STORY.get(), d);
    }

    private static void reward(ServerPlayer p) {
        StoryData d = Progress.d(p);
        int tier = d.questTier, n = Math.max(1, parse(d).size()), total = d.questNeed;
        boolean elite = tier == 4;
        clearQuest(p);
        d = Progress.d(p);
        d.questsDone++;
        if (!elite) d.questCooldown = now(p.server) + COOLDOWN_DONE;
        d.cdFail = false;
        p.setData(ModRegistry.STORY.get(), d);
        var r = p.getRandom();
        int money = elite ? 500 : 60 + total * 6 + n * 40;
        int stars = elite ? 0 : 1 + n / 2;
        int blessed = elite ? 2 : (r.nextFloat() < 0.12f * n ? 1 : 0);
        Progress.addMoney(p, money, true);
        if (stars > 0) com.pvpciv.inv.Bag.give(p, new ItemStack(ModRegistry.STAR_W1.get(), stars));
        if (r.nextFloat() < (elite ? 0.4f : 0.04f)) {   // звезда крови: элитный контракт ~40%, обычный 4%
            ItemStack bs = new ItemStack(ModRegistry.BLOOD_STAR_W1.get());
            com.pvpciv.inv.Bag.give(p, bs);
            Story.announceRareDrop(p.level(), bs, p.getGameProfile().getName());
        }
        if (blessed > 0) {
            ItemStack b = new ItemStack(ModRegistry.BLESSED_STAR_W1.get(), blessed);
            com.pvpciv.inv.Bag.give(p, b);
            Story.announceRareDrop(p.level(), b, p.getGameProfile().getName());
        }
        p.sendSystemMessage(Component.literal("Плата Кармиллы: " + money + " ✦" + (stars > 0 ? ", звёзды ×" + stars : "") + (blessed > 0 ? ", благословенные ×" + blessed : ""))
                .withStyle(s -> s.withColor(C)));
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 0.8f);
        if (d.questsDone >= 5) Titles.grant(p, "contractor");
        Story.sync(p);
    }

    // ------------------------------------------------------------------ похищение
    private static final java.util.Set<java.util.UUID> ABDUCTING = new java.util.HashSet<>();

    /** Катсцена: Кармилла появляется за спиной, хватает, темнота — подвал, укус, клетка. */
    public static void abduct(ServerPlayer p) {
        boolean eliteFail = Progress.d(p).questTier == 4 && hasQuest(Progress.d(p));
        abduct(p, eliteFail ? JAIL * 10 / 3 : JAIL, true);
    }

    /** jailTicks — сколько сидеть; dropQuest — сгорает ли контракт (для звезды крови — нет). */
    public static void abduct(ServerPlayer p, long jailTicks, boolean dropQuest) { abduct(p, jailTicks, dropQuest, false); }

    /** Похищение по жалобе Элеоноры (звонок во время охоты): у Кармиллы другие реплики. */
    public static final java.util.Set<java.util.UUID> ELE_CALL = new java.util.HashSet<>();

    public static void abduct(ServerPlayer p, long jailTicks, boolean dropQuest, boolean blood) {
        MinecraftServer server = p.server;
        final boolean ele = ELE_CALL.remove(p.getUUID());
        if (ABDUCTING.contains(p.getUUID())) return;
        if (dropQuest) clearQuest(p);
        ABDUCTING.add(p.getUUID());
        DialogueManager.forget(p);
        PacketDistributor.sendToPlayer(p, new Network.DialogueClose());
        if (p.isPassenger()) p.stopRiding();
        // бета: на время катсцены — неуязвимость, мобы рядом уходят (исчезают и потом заново появятся на своих точках)
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 240, 4, false, false));
        p.getPersistentData().putLong("abductSafeUntil", p.serverLevel().getGameTime() + 240);
        for (net.minecraft.world.entity.Mob m : p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(20),
                m -> com.pvpciv.combat.Mobs.level(m) > 0 && !(m instanceof com.pvpciv.entity.boss.CivBoss))) {
            p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, m.getX(), m.getY() + 1, m.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
            m.discard();
        }
        if (!CivWorld.is(p.level())) toCell(p);
        ServerLevel lvl = CivWorld.level(server);
        // 0.7: у каждой жертвы своя Кармилла (видна только ей), катсцена ~11 с; саму жертву на это время не видно
        Vec3 look0 = p.getLookAngle().multiply(1, 0, 1);
        final Vec3 look = look0.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : look0.normalize();
        Vec3 at = p.position().add(look.scale(3.2));
        CarmillaEntity c = CarmillaEntity.phantom(lvl, p, at);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 110, 10, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, 110, 128, false, false));   // прыжок отключён
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 230, 0, false, false));
        PacketDistributor.sendToPlayer(p, new Network.Cinematic(1, 110, blood ? "Звезда крови требует платы…" : ele ? "Элеонора нажаловалась…" : "Кармилла пришла за долгом…"));
        lvl.playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2f, 0.7f);
        if (c != null) {
            CarmillaEntity.puff(lvl, at);
            c.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
            c.setAnim("spread");
            lvl.playSound(null, p.blockPosition(), SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 1.5f, 0.6f);
        }
        Story.say(p, NAME, C, blood ? "Звезда на крови треснула? Значит, кровь — моя." : ele ? "Мне тут позвонили. Говорят, ты грубишь хорошим девочкам." : "Попался.");
        final Vec3 side = new Vec3(-look.z, 0, look.x);
        GameEvents.later(server, 35, () -> {
            if (c == null || !p.isAlive()) return;
            Vec3 s2 = p.position().add(side.scale(2.2)).add(look.scale(1.2));
            CarmillaEntity.puff(lvl, c.position());
            c.teleportTo(s2.x, p.getY(), s2.z);
            c.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
            c.setAnim("cross");
            lvl.playSound(null, p.blockPosition(), SoundEvents.BAT_AMBIENT, SoundSource.HOSTILE, 1f, 0.6f);
            Story.say(p, NAME, C, blood ? "Ты же читал мелкий шрифт? Звезда — моя кровь. Провал — твоя." : ele ? "Элеонора всхлипывала в трубку десять минут. Ты в курсе, что она звонит МНЕ, когда обижается?" : "Двадцать минут. Ты потратил их на что угодно, кроме меня. Обидно, знаешь ли.");
        });
        GameEvents.later(server, 70, () -> {
            if (c == null || !p.isAlive()) return;
            Vec3 s3 = p.position().add(look.scale(1.1));
            c.teleportTo(s3.x, p.getY(), s3.z);
            c.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
            c.setAnim("drag");
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
            lvl.sendParticles(new DustParticleOptions(new Vector3f(0.7f, 0, 0.1f), 2f), p.getX(), p.getY() + 1, p.getZ(), 60, 0.5, 0.8, 0.5, 0);
            lvl.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 1.2f, 1.3f);
            Story.say(p, NAME, C, "Не дёргайся. Пойдём вниз — там тихо и никто не мешает.");
        });
        GameEvents.later(server, 105, () -> {
            if (!p.isAlive() || p.hasDisconnected()) { ABDUCTING.remove(p.getUUID()); if (c != null) c.discard(); return; }
            StoryData d = Progress.d(p);
            d.jailUntil = real() + jailTicks;
            if (hasQuest(d) && d.questDeadline > 0) d.questDeadline += jailTicks;   // время в клетке не съедает контракт
            d.jailed++;
            d.questCooldown = 0;
            p.setData(ModRegistry.STORY.get(), d);
            Basement.setDoor(lvl, false);
            toCell(p);
            if (c != null) {
                Vec3 cs = Vec3.atBottomCenterOf(Basement.CELL_SPOT);
                c.teleportTo(cs.x - 1.2, cs.y, cs.z);
                c.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
                c.setAnim("bite");
            }
            PacketDistributor.sendToPlayer(p, new Network.Cinematic(2, 70, "Укус"));
            lvl.playSound(null, p.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.HOSTILE, 1.5f, 0.6f);
            lvl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.PLAYERS, 1f, 0.8f);
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0, false, false));
            p.getFoodData().setFoodLevel(0);          // выпила досуха — жизнь мёдом не покажется
            p.getFoodData().setSaturation(0);
        });
        GameEvents.later(server, 175, () -> {
            if (c == null || !p.isAlive()) return;
            c.setAnim("spread");
            lvl.playSound(null, p.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 1f, 0.7f);
            Story.say(p, NAME, C, ele ? "М-м-м… Вкусно. Передам Элеоноре, что ты извиняешься." : "М-м-м… Вкусно. Даже жаль, что ты так редко опаздываешь.");
        });
        GameEvents.later(server, 220, () -> {
            ABDUCTING.remove(p.getUUID());
            if (c != null) { CarmillaEntity.puff(lvl, c.position()); c.discard(); }
            p.removeEffect(MobEffects.INVISIBILITY);
            long mins = Math.max(1, jailTicks / 1200);
            Story.say(p, NAME, C, blood
                    ? "Оружие твоё цело — я держу слово. А ты посидишь у меня " + mins + " минут. Сделка есть сделка."
                    : "Посиди тут " + mins + " " + (mins < 5 ? "минутки" : "минут") + ", остынь. Дверь откроется сама — я же не зверь.");
            Titles.grant(p, "vampire_victim");
            Story.sync(p);
        });
    }

    public static void toCell(ServerPlayer p) {
        ServerLevel lvl = CivWorld.level(p.server);
        Vec3 cs = Vec3.atBottomCenterOf(Basement.CELL_SPOT);
        p.resetFallDistance();
        p.teleportTo(lvl, cs.x, cs.y, cs.z, 90, 0);
        p.setDeltaMovement(Vec3.ZERO);
    }

    public static void release(ServerPlayer p) {
        StoryData d = Progress.d(p);
        d.jailUntil = 0;
        d.questCooldown = now(p.server) + COOLDOWN_FAIL;
        d.cdFail = true;
        p.setData(ModRegistry.STORY.get(), d);
        boolean other = false;
        for (ServerPlayer o : p.server.getPlayerList().getPlayers()) if (o != p && jailed(o)) other = true;
        ServerLevel lvl = CivWorld.level(p.server);
        if (!other) Basement.setDoor(lvl, true);
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 120, 0));
        Story.say(p, NAME, C, "Свободен. Пока что. Анемия пройдёт через пару минут — съешь что-нибудь красное.");
        lvl.playSound(null, Basement.CELL_DOOR, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1f, 0.8f);
        Story.sync(p);
    }

    /** Выход: запомнить остаток срока контракта (пока игрока нет, срок не идёт). */
    public static void onLogout(ServerPlayer p) {
        StoryData d = Progress.d(p);
        if (hasQuest(d) && d.questDeadline > 0) { d.questLeft = Math.max(20 * 60, d.questDeadline - real()); p.setData(ModRegistry.STORY.get(), d); }
    }

    /** Вход в игру: должник возвращается в клетку; просроченный контракт — Кармилла придёт чуть позже. */
    public static void onLogin(ServerPlayer p) {
        StoryData d = Progress.d(p);
        // переход со старых игровых тиков (до 0.8.6) на реальное время
        long shift = real() - CivWorld.level(p.server).getGameTime();
        if (d.questCooldown > 0 && d.questCooldown < 10_000_000_000L) d.questCooldown += shift;
        if (d.eliteCd > 0 && d.eliteCd < 10_000_000_000L) d.eliteCd += shift;
        if (d.questDeadline > 0 && d.questDeadline < 10_000_000_000L) d.questDeadline += shift;
        // срок контракта на паузе, пока игрока нет
        if (d.questLeft > 0 && hasQuest(d)) d.questDeadline = real() + d.questLeft;
        d.questLeft = 0;
        p.setData(ModRegistry.STORY.get(), d);
        if (jailed(p)) { Basement.setDoor(CivWorld.level(p.server), false); toCell(p); }
        else if (Progress.d(p).jailUntil > 0) release(p);
    }
}
